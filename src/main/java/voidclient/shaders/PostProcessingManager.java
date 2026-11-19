package voidclient.shaders;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.MappableRingBuffer;
import voidclient.VoidClient;
import voidclient.gui.Notifications;
import voidclient.modules.visuals.Bloom;
import voidclient.modules.visuals.DepthEffects;
import voidclient.modules.visuals.MotionBlur;
import voidclient.utilities.ColorUtil;

import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Void post-processing engine. Runs its own full-screen passes on the main framebuffer:
 * depth effects before the hand is drawn, then motion blur, bloom and the colour/screen
 * uber-pass after the world. Each failure disables post-processing instead of crashing.
 */
public final class PostProcessingManager {
    private static final PostProcessingManager INSTANCE = new PostProcessingManager();

    public static PostProcessingManager get() { return INSTANCE; }

    private final PostTarget scene = new PostTarget("scene");
    private final PostTarget bloomA = new PostTarget("bloom A");
    private final PostTarget bloomB = new PostTarget("bloom B");
    private final PostTarget history = new PostTarget("history");
    private MappableRingBuffer uberBuf, depthBuf, bloomBuf, blurH, blurV, motionBuf;
    private boolean failed;
    private long frameNanos = System.nanoTime();

    private PostProcessingManager() {}

    private static MappableRingBuffer ring(String name, int size) {
        return new MappableRingBuffer(() -> "Void " + name, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, size);
    }

    private void ensureBuffers() {
        if (uberBuf != null) return;
        uberBuf = ring("uber", 13 * 16);
        depthBuf = ring("depth", 4 * 16);
        bloomBuf = ring("bloom", 3 * 16);
        blurH = ring("blur h", 16);
        blurV = ring("blur v", 16);
        motionBuf = ring("motion", 16);
    }

    public boolean hasFailed() { return failed; }

    public void resetFailure() { failed = false; }

    private void fail(Throwable t) {
        failed = true;
        VoidClient.LOG.error("Void post-processing failed; disabling post effects until re-enabled", t);
        Notifications.error("Post-processing", "Shader error - effects disabled (see log)");
    }

    // ---- passes --------------------------------------------------------------------------

    private void write(CommandEncoder enc, MappableRingBuffer buf, Consumer<Std140Builder> writer) {
        try (GpuBuffer.MappedView v = enc.mapBuffer(buf.getBlocking(), false, true)) {
            writer.accept(Std140Builder.intoBuffer(v.data()));
        }
    }

    private void pass(CommandEncoder enc, String label, RenderPipeline pipeline, GpuTextureView target, String uniform, MappableRingBuffer params,
                      String[] samplers, GpuTextureView[] inputs) {
        try (RenderPass p = enc.createRenderPass(() -> "Void " + label, target, OptionalInt.empty())) {
            p.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(p);
            p.setUniform(uniform, params.getBlocking());
            for (int i = 0; i < samplers.length; i++) {
                p.bindTexture(samplers[i], inputs[i], RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            }
            p.draw(0, 3);
        }
    }

    private static void copy(CommandEncoder enc, Framebuffer main, PostTarget dst) {
        enc.copyTextureToTexture(main.getColorAttachment(), dst.texture, 0, 0, 0, 0, 0, dst.width, dst.height);
    }

    /** Depth fog + depth of field; called right before the depth buffer is cleared for the hand. */
    public void renderDepthPass() {
        if (failed) return;
        DepthEffects d = DepthEffects.INSTANCE;
        if (d == null || !d.isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            ensureBuffers();
            Framebuffer main = mc.getFramebuffer();
            CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
            scene.ensure(main.textureWidth, main.textureHeight);
            copy(enc, main, scene);
            float far = mc.gameRenderer.getFarPlaneDistance();
            int fog = d.fogColor();
            write(enc, depthBuf, b -> b
                    .putVec4((float) d.fogStart(), (float) d.fogEnd(), d.fogEnabled() ? (float) d.fogStrength() : 0f, d.fogEnabled() ? 1f : 0f)
                    .putVec4(ColorUtil.red(fog) / 255f, ColorUtil.green(fog) / 255f, ColorUtil.blue(fog) / 255f, 0f)
                    .putVec4((float) d.focusDistance(), (float) d.focusRange(), (float) d.maxBlur(), d.dofEnabled() ? 1f : 0f)
                    .putVec4(0.05f, far, main.textureWidth, main.textureHeight));
            pass(enc, "depth", ShaderManager.DEPTH, main.getColorAttachmentView(), "VoidDepth", depthBuf,
                    new String[]{"InSampler", "DepthSampler"}, new GpuTextureView[]{scene.view, main.getDepthAttachmentView()});
            depthBuf.rotate();
        } catch (Throwable t) {
            fail(t);
        }
    }

    /** Motion blur, bloom and the uber pass; called after the world and entity outlines. */
    public void render(float tickDelta) {
        if (failed) return;
        long now = System.nanoTime();
        float dt = Math.min(0.25f, (now - frameNanos) / 1e9f);
        frameNanos = now;
        MinecraftClient mc = MinecraftClient.getInstance();
        MotionBlur motion = MotionBlur.INSTANCE;
        Bloom bloom = Bloom.INSTANCE;
        UberParams uber = UberParams.collect();
        boolean doMotion = motion != null && motion.isEnabled();
        boolean doBloom = bloom != null && bloom.isEnabled();
        if (!doMotion && !doBloom && !uber.any()) {
            if (history.texture != null) history.close();
            return;
        }
        try {
            ensureBuffers();
            Framebuffer main = mc.getFramebuffer();
            int w = main.textureWidth, h = main.textureHeight;
            CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
            scene.ensure(w, h);

            if (doMotion) {
                history.ensure(w, h);
                if (!history.initialised) {
                    copy(enc, main, history);
                    history.initialised = true;
                }
                copy(enc, main, scene);
                float weight = motion.historyWeight(dt);
                write(enc, motionBuf, b -> b.putVec4(weight, 0, 0, 0));
                pass(enc, "motion blur", ShaderManager.MOTION, main.getColorAttachmentView(), "VoidMotion", motionBuf,
                        new String[]{"InSampler", "HistorySampler"}, new GpuTextureView[]{scene.view, history.view});
                motionBuf.rotate();
                copy(enc, main, history);
            } else if (history.texture != null) {
                history.close();
            }

            if (doBloom) {
                int div = bloom.downscale();
                int bw = Math.max(1, w / div), bh = Math.max(1, h / div);
                bloomA.ensure(bw, bh);
                bloomB.ensure(bw, bh);
                copy(enc, main, scene);
                int tint = bloom.glowColor();
                write(enc, bloomBuf, b -> b
                        .putVec4((float) bloom.threshold(), 0.5f, (float) bloom.intensity(), (float) bloom.saturation())
                        .putVec4(ColorUtil.red(tint) / 255f, ColorUtil.green(tint) / 255f, ColorUtil.blue(tint) / 255f, (float) bloom.tintMix())
                        .putVec4(w, h, bw, bh));
                pass(enc, "bloom extract", ShaderManager.BLOOM_EXTRACT, bloomA.view, "VoidBloom", bloomBuf,
                        new String[]{"InSampler"}, new GpuTextureView[]{scene.view});
                float spread = (float) bloom.radius();
                write(enc, blurH, b -> b.putVec4(1, 0, spread / bw, spread / bh));
                write(enc, blurV, b -> b.putVec4(0, 1, spread / bw, spread / bh));
                for (int i = 0; i < bloom.iterations(); i++) {
                    pass(enc, "bloom blur h", ShaderManager.BLUR, bloomB.view, "VoidBlur", blurH, new String[]{"InSampler"}, new GpuTextureView[]{bloomA.view});
                    pass(enc, "bloom blur v", ShaderManager.BLUR, bloomA.view, "VoidBlur", blurV, new String[]{"InSampler"}, new GpuTextureView[]{bloomB.view});
                }
                pass(enc, "bloom composite", ShaderManager.BLOOM_COMPOSITE, main.getColorAttachmentView(), "VoidBloom", bloomBuf,
                        new String[]{"InSampler"}, new GpuTextureView[]{bloomA.view});
                bloomBuf.rotate();
                blurH.rotate();
                blurV.rotate();
            }

            if (uber.any()) {
                copy(enc, main, scene);
                float time = (System.currentTimeMillis() % 3_600_000L) / 1000f;
                write(enc, uberBuf, b -> uber.write(b, w, h, time));
                pass(enc, "uber", ShaderManager.UBER, main.getColorAttachmentView(), "VoidUber", uberBuf,
                        new String[]{"InSampler"}, new GpuTextureView[]{scene.view});
                uberBuf.rotate();
            }
        } catch (Throwable t) {
            fail(t);
        }
    }
}

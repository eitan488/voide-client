package voidclient.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import voidclient.VoidClient;
import voidclient.events.Events;
import voidclient.utilities.ColorUtil;

/**
 * World-space drawing in camera-relative coordinates (y up, right-handed, degrees for
 * rotations). "X-ray" variants disable depth testing so ESP is visible through walls.
 */
public final class RenderManager {
    private RenderManager() {}

    public static final RenderPipeline XRAY_LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of(VoidClient.MOD_ID, "pipeline/xray_lines"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build());
    public static final RenderPipeline XRAY_QUADS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of(VoidClient.MOD_ID, "pipeline/xray_quads"))
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());
    public static final RenderPipeline WORLD_LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of(VoidClient.MOD_ID, "pipeline/world_lines"))
                    .withDepthWrite(false)
                    .build());
    public static final RenderPipeline WORLD_QUADS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of(VoidClient.MOD_ID, "pipeline/world_quads"))
                    .withCull(false)
                    .build());

    public static final RenderLayer XRAY_LINES = RenderLayer.of("voidclient_xray_lines", RenderSetup.builder(XRAY_LINES_PIPELINE).translucent().build());
    public static final RenderLayer XRAY_QUADS = RenderLayer.of("voidclient_xray_quads", RenderSetup.builder(XRAY_QUADS_PIPELINE).translucent().build());
    public static final RenderLayer WORLD_LINES = RenderLayer.of("voidclient_world_lines", RenderSetup.builder(WORLD_LINES_PIPELINE).translucent().build());
    public static final RenderLayer WORLD_QUADS = RenderLayer.of("voidclient_world_quads", RenderSetup.builder(WORLD_QUADS_PIPELINE).translucent().build());

    private static Vec3d cam = Vec3d.ZERO;
    private static VertexConsumerProvider consumers;
    private static MatrixStack matrices;

    public static void begin(Events.WorldRender e) {
        cam = e.cameraPos;
        consumers = e.consumers;
        matrices = e.matrices;
    }

    public static void end(Events.WorldRender e) {
        consumers = null;
        matrices = null;
    }

    public static Vec3d camera() { return cam; }

    private static VertexConsumer lines(boolean xray) { return consumers.getBuffer(xray ? XRAY_LINES : WORLD_LINES); }

    private static VertexConsumer quads(boolean xray) { return consumers.getBuffer(xray ? XRAY_QUADS : WORLD_QUADS); }

    private static boolean ready() { return consumers != null && matrices != null; }

    // ---- boxes ---------------------------------------------------------------------------

    public static void boxOutline(Box box, int color, float width, boolean xray) {
        if (!ready() || ColorUtil.alpha(color) == 0) return;
        Box b = box.offset(-cam.x, -cam.y, -cam.z);
        MatrixStack.Entry e = matrices.peek();
        VertexConsumer vc = lines(xray);
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ, x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        edge(vc, e, x1, y1, z1, x2, y1, z1, color, width);
        edge(vc, e, x1, y1, z2, x2, y1, z2, color, width);
        edge(vc, e, x1, y2, z1, x2, y2, z1, color, width);
        edge(vc, e, x1, y2, z2, x2, y2, z2, color, width);
        edge(vc, e, x1, y1, z1, x1, y2, z1, color, width);
        edge(vc, e, x2, y1, z1, x2, y2, z1, color, width);
        edge(vc, e, x1, y1, z2, x1, y2, z2, color, width);
        edge(vc, e, x2, y1, z2, x2, y2, z2, color, width);
        edge(vc, e, x1, y1, z1, x1, y1, z2, color, width);
        edge(vc, e, x2, y1, z1, x2, y1, z2, color, width);
        edge(vc, e, x1, y2, z1, x1, y2, z2, color, width);
        edge(vc, e, x2, y2, z1, x2, y2, z2, color, width);
    }

    private static void edge(VertexConsumer vc, MatrixStack.Entry e, float x1, float y1, float z1, float x2, float y2, float z2, int color, float width) {
        Vector3f n = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        if (n.lengthSquared() < 1e-12f) return;
        n.normalize();
        vc.vertex(e, x1, y1, z1).color(color).normal(e, n).lineWidth(width);
        vc.vertex(e, x2, y2, z2).color(color).normal(e, n).lineWidth(width);
    }

    public static void line(Vec3d a, Vec3d b, int color, float width, boolean xray) {
        if (!ready()) return;
        edge(lines(xray), matrices.peek(), (float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z),
                (float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z), color, width);
    }

    public static void boxFilled(Box box, int color, boolean xray) {
        if (!ready() || ColorUtil.alpha(color) == 0) return;
        Box b = box.offset(-cam.x, -cam.y, -cam.z);
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = quads(xray);
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ, x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        quad(vc, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
        quad(vc, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
        quad(vc, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        quad(vc, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        quad(vc, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        quad(vc, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int color) {
        vc.vertex(m, ax, ay, az).color(color);
        vc.vertex(m, bx, by, bz).color(color);
        vc.vertex(m, cx, cy, cz).color(color);
        vc.vertex(m, dx, dy, dz).color(color);
    }

    public static void box(Box box, int fill, int outline, float width, boolean xray) {
        boxFilled(box, fill, xray);
        boxOutline(box, outline, width, xray);
    }

    /** Horizontal quad at height y (used for chunk floors, light-level squares). */
    public static void flatQuad(double x1, double z1, double x2, double z2, double y, int color, boolean xray) {
        if (!ready()) return;
        Matrix4f m = matrices.peek().getPositionMatrix();
        float fy = (float) (y - cam.y);
        quad(quads(xray), m, (float) (x1 - cam.x), fy, (float) (z1 - cam.z), (float) (x1 - cam.x), fy, (float) (z2 - cam.z),
                (float) (x2 - cam.x), fy, (float) (z2 - cam.z), (float) (x2 - cam.x), fy, (float) (z1 - cam.z), color);
    }

    // ---- rings & beams -------------------------------------------------------------------

    public static void ring(Vec3d center, double radius, int color, float width, int segments, boolean xray) {
        if (!ready()) return;
        Vec3d prev = null;
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2 * i / segments;
            Vec3d p = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            if (prev != null) line(prev, p, color, width, xray);
            prev = p;
        }
    }

    /** Filled horizontal disc with radial fade (aura / explosion radius). */
    public static void disc(Vec3d center, double radius, int innerColor, int outerColor, int segments, boolean xray) {
        if (!ready()) return;
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = quads(xray);
        float cx = (float) (center.x - cam.x), cy = (float) (center.y - cam.y), cz = (float) (center.z - cam.z);
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments, a2 = Math.PI * 2 * (i + 1) / segments;
            float x1 = cx + (float) (Math.cos(a1) * radius), z1 = cz + (float) (Math.sin(a1) * radius);
            float x2 = cx + (float) (Math.cos(a2) * radius), z2 = cz + (float) (Math.sin(a2) * radius);
            vc.vertex(m, cx, cy, cz).color(innerColor);
            vc.vertex(m, cx, cy, cz).color(innerColor);
            vc.vertex(m, x2, cy, z2).color(outerColor);
            vc.vertex(m, x1, cy, z1).color(outerColor);
        }
    }

    /** Vertical beam (square prism) with a vertical fade. */
    public static void beam(Vec3d base, double height, double halfWidth, int bottomColor, int topColor, boolean xray) {
        if (!ready()) return;
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = quads(xray);
        float x1 = (float) (base.x - halfWidth - cam.x), x2 = (float) (base.x + halfWidth - cam.x);
        float z1 = (float) (base.z - halfWidth - cam.z), z2 = (float) (base.z + halfWidth - cam.z);
        float y1 = (float) (base.y - cam.y), y2 = (float) (base.y + height - cam.y);
        float[][] sides = {{x1, z1, x2, z1}, {x2, z1, x2, z2}, {x2, z2, x1, z2}, {x1, z2, x1, z1}};
        for (float[] s : sides) {
            vc.vertex(m, s[0], y1, s[1]).color(bottomColor);
            vc.vertex(m, s[2], y1, s[3]).color(bottomColor);
            vc.vertex(m, s[2], y2, s[3]).color(topColor);
            vc.vertex(m, s[0], y2, s[1]).color(topColor);
        }
    }

    /** Camera-facing quad (particles, sprites). */
    public static void billboard(Vec3d pos, float size, int color, boolean xray) {
        if (!ready()) return;
        var camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Matrix4f m = new Matrix4f(matrices.peek().getPositionMatrix())
                .translate((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z))
                .rotate(camera.getRotation());
        float h = size / 2f;
        VertexConsumer vc = quads(xray);
        vc.vertex(m, -h, -h, 0).color(color);
        vc.vertex(m, -h, h, 0).color(color);
        vc.vertex(m, h, h, 0).color(color);
        vc.vertex(m, h, -h, 0).color(color);
    }

    // ---- text ----------------------------------------------------------------------------

    /**
     * Billboarded text centred at pos. {@code scale} is world units per text pixel at
     * distance 1 when {@code distanceScaled}, else absolute.
     */
    public static void text(Text text, Vec3d pos, float scale, int color, int background, boolean xray, boolean distanceScaled) {
        if (!ready()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        double dist = pos.distanceTo(cam);
        float s = distanceScaled ? (float) (scale * Math.max(1.0, dist) * 0.1) : scale;
        Matrix4f m = new Matrix4f(matrices.peek().getPositionMatrix())
                .translate((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z))
                .rotate(mc.gameRenderer.getCamera().getRotation())
                .scale(s, -s, s);
        float w = tr.getWidth(text);
        tr.draw(text, -w / 2f, 0, color, false, m, consumers,
                xray ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL,
                background, LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    public static void text(String text, Vec3d pos, float scale, int color, int background, boolean xray) {
        text(Text.literal(text), pos, scale, color, background, xray, true);
    }
}

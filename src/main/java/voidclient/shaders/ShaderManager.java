package voidclient.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import voidclient.VoidClient;

/**
 * Void post-processing pipelines. Shaders live in assets/voidclient/shaders/post and use the
 * vanilla full-screen-triangle vertex shader (minecraft:core/screenquad).
 */
public final class ShaderManager {
    private ShaderManager() {}

    private static RenderPipeline post(String name, String uniform, BlendFunction blend, String... samplers) {
        RenderPipeline.Builder b = RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
                .withLocation(Identifier.of(VoidClient.MOD_ID, "pipeline/post_" + name))
                .withVertexShader(Identifier.ofVanilla("core/screenquad"))
                .withFragmentShader(Identifier.of(VoidClient.MOD_ID, "post/" + name))
                .withUniform("Globals", UniformType.UNIFORM_BUFFER)
                .withUniform(uniform, UniformType.UNIFORM_BUFFER);
        for (String s : samplers) b.withSampler(s);
        if (blend != null) b.withBlend(blend);
        return b.build();
    }

    public static final RenderPipeline UBER = post("uber", "VoidUber", null, "InSampler");
    public static final RenderPipeline DEPTH = post("depth", "VoidDepth", null, "InSampler", "DepthSampler");
    public static final RenderPipeline BLOOM_EXTRACT = post("bloom_extract", "VoidBloom", null, "InSampler");
    public static final RenderPipeline BLUR = post("blur", "VoidBlur", null, "InSampler");
    public static final RenderPipeline BLOOM_COMPOSITE = post("bloom_composite", "VoidBloom", BlendFunction.ADDITIVE, "InSampler");
    public static final RenderPipeline MOTION = post("motion_blur", "VoidMotion", null, "InSampler", "HistorySampler");
}

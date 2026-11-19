package voidclient.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Arbitrary coloured geometry for the 1.21.11 GUI renderer. Vertices are packed as
 * (x, y, argb) triples and emitted as quads (triangles are degenerate quads).
 */
public record ShapeElement(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float[] data,
        int vertexCount,
        @Nullable ScreenRect scissorArea,
        @Nullable ScreenRect bounds
) implements SimpleGuiElementRenderState {
    @Override
    public void setupVertices(VertexConsumer vertices) {
        float[] d = data;
        for (int i = 0, n = vertexCount * 3; i < n; i += 3) {
            vertices.vertex(pose, d[i], d[i + 1]).color(Float.floatToRawIntBits(d[i + 2]));
        }
    }
}

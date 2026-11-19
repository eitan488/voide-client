package voidclient.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

/** Owned RGBA8 render target, recreated on resize. */
final class PostTarget implements AutoCloseable {
    private final String label;
    GpuTexture texture;
    GpuTextureView view;
    int width, height;
    boolean initialised;

    PostTarget(String label) { this.label = label; }

    boolean ensure(int w, int h) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        if (texture != null && w == width && h == height) return false;
        close();
        int usage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
        texture = RenderSystem.getDevice().createTexture(() -> "Void " + label, usage, TextureFormat.RGBA8, w, h, 1, 1);
        view = RenderSystem.getDevice().createTextureView(texture);
        width = w;
        height = h;
        initialised = false;
        return true;
    }

    @Override
    public void close() {
        if (view != null) view.close();
        if (texture != null) texture.close();
        view = null;
        texture = null;
        width = height = 0;
        initialised = false;
    }
}

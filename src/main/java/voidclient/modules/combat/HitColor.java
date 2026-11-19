package voidclient.modules.combat;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ColorSetting;
import voidclient.events.Events;
import voidclient.mixin.OverlayTextureAccessor;

/** Recolours the red hurt flash on entities by rewriting the overlay texture. */
public class HitColor extends Module {
    private static final int VANILLA = 0xB3FF0000;

    public final ColorSetting color = add(new ColorSetting("Color", "Hurt overlay colour; alpha sets the intensity.", 0xB3B026FF));
    private int applied = VANILLA;

    public HitColor() {
        super("Hit Color", "Change the colour entities flash when hurt.", Category.COMBAT);
        alsoListIn(Category.VISUALS);
        on(Events.TickPost.class, e -> {
            int c = color.getColor();
            if (c != applied) write(c);
        });
    }

    @Override
    protected void onEnable() { write(color.getColor()); }

    @Override
    protected void onDisable() { write(VANILLA); }

    private void write(int argb) {
        if (mc.gameRenderer == null) return;
        NativeImageBackedTexture tex = ((OverlayTextureAccessor) mc.gameRenderer.getOverlayTexture()).voidclient$texture();
        NativeImage img = tex.getImage();
        if (img == null) return;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setColorArgb(x, y, argb);
        tex.upload();
        applied = argb;
    }
}

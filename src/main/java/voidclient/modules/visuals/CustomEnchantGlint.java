package voidclient.modules.visuals;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Recolours the enchantment glint by replacing the glint textures with tinted copies,
 * and adjusts glint strength/speed through the vanilla accessibility options.
 */
public class CustomEnchantGlint extends Module {
    private static final Identifier[] GLINTS = {ItemRenderer.ITEM_ENCHANTMENT_GLINT, ItemRenderer.ENTITY_ENCHANTMENT_GLINT};

    public final ColorSetting color = add(new ColorSetting("Color", "Glint tint (chroma supported).", 0xFFB026FF, false));
    public final NumberSetting brightness = add(new NumberSetting("Brightness", "Tint brightness.", 1.4, 0.2, 3, 0.05));
    public final NumberSetting strength = add(new NumberSetting("Strength", "Glint visibility (vanilla option).", 1, 0, 1, 0.05));
    public final NumberSetting speed = add(new NumberSetting("Speed", "Glint animation speed (vanilla option).", 0.5, 0, 1, 0.05));

    private final Map<Identifier, NativeImage> originals = new HashMap<>();
    private final Map<Identifier, NativeImageBackedTexture> textures = new HashMap<>();
    private int appliedColor;
    private double prevStrength = -1, prevSpeed = -1;

    public CustomEnchantGlint() {
        super("Custom Enchantment Glint", "Recolour, dim or speed up the enchantment glint.", Category.VISUALS);
        on(Events.TickPost.class, e -> {
            int c = color.getColor();
            boolean replaced = false;
            for (Identifier id : GLINTS) if (mc.getTextureManager().getTexture(id) != textures.get(id)) replaced = true;
            if (c != appliedColor || replaced) tint(c);
            mc.options.getGlintStrength().setValue(strength.get());
            mc.options.getGlintSpeed().setValue(speed.get());
        });
    }

    private NativeImage original(Identifier id) {
        NativeImage img = originals.get(id);
        if (img != null) return img;
        try {
            Optional<Resource> res = mc.getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().getInputStream()) {
                img = NativeImage.read(in);
            }
            originals.put(id, img);
            return img;
        } catch (Exception e) {
            VoidClient.LOG.warn("Could not read glint texture {}", id, e);
            return null;
        }
    }

    private void tint(int color) {
        appliedColor = color;
        float r = ((color >> 16) & 255) / 255f * brightness.getFloat(), g = ((color >> 8) & 255) / 255f * brightness.getFloat(), b = (color & 255) / 255f * brightness.getFloat();
        for (Identifier id : GLINTS) {
            NativeImage src = original(id);
            if (src == null) continue;
            NativeImageBackedTexture tex = textures.get(id);
            if (tex == null || tex.getImage() == null) {
                tex = new NativeImageBackedTexture(() -> "Void glint", src.getWidth(), src.getHeight(), false);
                textures.put(id, tex);
            }
            NativeImage dst = tex.getImage();
            for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getColorArgb(x, y);
                float lum = (((p >> 16) & 255) * 0.3f + ((p >> 8) & 255) * 0.59f + (p & 255) * 0.11f) / 255f;
                int nr = Math.min(255, Math.round(lum * r * 255)), ng = Math.min(255, Math.round(lum * g * 255)), nb = Math.min(255, Math.round(lum * b * 255));
                dst.setColorArgb(x, y, (p & 0xFF000000) | (nr << 16) | (ng << 8) | nb);
            }
            tex.upload();
            if (mc.getTextureManager().getTexture(id) != tex) mc.getTextureManager().registerTexture(id, tex);
        }
    }

    @Override
    protected void onEnable() {
        prevStrength = mc.options.getGlintStrength().getValue();
        prevSpeed = mc.options.getGlintSpeed().getValue();
        tint(color.getColor());
    }

    @Override
    protected void onDisable() {
        for (Identifier id : GLINTS) {
            NativeImage src = originals.get(id);
            NativeImageBackedTexture tex = textures.get(id);
            if (src != null && tex != null && tex.getImage() != null) {
                tex.getImage().copyFrom(src);
                tex.upload();
            }
        }
        appliedColor = 0;
        if (prevStrength >= 0) mc.options.getGlintStrength().setValue(prevStrength);
        if (prevSpeed >= 0) mc.options.getGlintSpeed().setValue(prevSpeed);
    }
}

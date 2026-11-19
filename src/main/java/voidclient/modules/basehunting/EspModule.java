package voidclient.modules.basehunting;

import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/**
 * Shared ESP settings: colour, outline, fill, transparency, distance labels, text size,
 * label background, render distance, performance cap and through-walls.
 */
public abstract class EspModule extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", "Main ESP colour.", 0xFFB026FF, false));
    public final BooleanSetting outline = add(new BooleanSetting("Outline", "Draw box outlines.", true));
    public final NumberSetting outlineWidth = add(new NumberSetting("Outline Width", "Outline thickness.", 2, 0.5, 6, 0.5).visibleWhen(outline::get));
    public final BooleanSetting fill = add(new BooleanSetting("Fill", "Translucent box fill.", true));
    public final NumberSetting transparency = add(new NumberSetting("Transparency", "Fill transparency.", 0.8, 0, 0.98, 0.02).visibleWhen(fill::get));
    public final BooleanSetting distanceLabels = add(new BooleanSetting("Distance Labels", "Label with name and distance.", true));
    public final NumberSetting textSize = add(new NumberSetting("Text Size", "Label size.", 1, 0.3, 3, 0.05).visibleWhen(distanceLabels::get));
    public final BooleanSetting labelBackground = add(new BooleanSetting("Background", "Label background.", true).visibleWhen(distanceLabels::get));
    public final NumberSetting renderDistance = add(new NumberSetting("Render Distance", "Maximum ESP distance.", 96, 8, 512, 8).unit("m"));
    public final NumberSetting maxCount = add(new NumberSetting("Max Count", "Performance cap: nearest N targets.", 256, 8, 2000, 8));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Visible through blocks.", true));

    protected EspModule(String name, String description) {
        super(name, description, Category.BASE_HUNTING);
    }

    protected void drawBox(Box box, int c) {
        boolean xr = throughWalls.get();
        if (fill.get()) RenderManager.boxFilled(box, ColorUtil.multiplyAlpha(c, 1f - transparency.getFloat()), xr);
        if (outline.get()) RenderManager.boxOutline(box, c, outlineWidth.getFloat(), xr);
    }

    protected void drawLabel(String name, Vec3d at, int c) {
        if (!distanceLabels.get() || mc.player == null) return;
        double d = at.distanceTo(mc.player.getEyePos());
        String s = String.format(Locale.ROOT, "%s %.0fm", name, d);
        RenderManager.text(Text.literal(s), at, textSize.getFloat() * 0.22f, c | 0xFF000000, labelBackground.get() ? 0x90000000 : 0, throughWalls.get(), true);
    }

    protected boolean inRange(Vec3d p) {
        return mc.player != null && p.squaredDistanceTo(mc.player.getEyePos()) <= renderDistance.get() * renderDistance.get();
    }
}

package voidclient.modules.basehunting;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.Locale;

/** Chunk borders and floating statistic panels for inspected chunks above the threshold. */
public class ChunkInspectionEsp extends Module {
    public final BooleanSetting borders = add(new BooleanSetting("Chunk Borders", "Outline inspected chunks.", true));
    public final BooleanSetting panels = add(new BooleanSetting("Info Panels", "Floating statistics above chunks.", true));
    public final BooleanSetting distanceLabels = add(new BooleanSetting("Distance Labels", "Distance to each chunk in the panel.", true));
    public final BooleanSetting animated = add(new BooleanSetting("Animated Outlines", "Pulsing outline alpha.", true));
    public final BooleanSetting fill = add(new BooleanSetting("Floor Fill", "Translucent floor colour by dominant category.", true));
    public final NumberSetting panelHeight = add(new NumberSetting("Panel Height", "Panel height above the highest counted block.", 6, 0, 48, 1).unit("m"));
    public final NumberSetting rows = add(new NumberSetting("Panel Rows", "Max component rows per panel.", 6, 1, 20, 1));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Draw through terrain.", true));

    public ChunkInspectionEsp() {
        super("Chunk Inspection ESP", "Visual overlay for inspected chunks: borders, colours and floating stats.", Category.BASE_HUNTING);
        on(Events.WorldRender.class, e -> render());
    }

    private void render() {
        ChunkIntelligence ci = ChunkIntelligence.INSTANCE;
        if (ci == null || mc.player == null || mc.world == null) return;
        if (!ci.isEnabled()) return;
        ChunkPos pc = mc.player.getChunkPos();
        int range = ci.displayDistance.getInt();
        float alpha = 1f - ci.transparency.getFloat();
        float pulse = animated.get() ? (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 300.0)) : 1f;
        boolean xr = throughWalls.get();
        for (ChunkStats s : ci.scanner().results().values()) {
            if (Math.max(Math.abs(s.cx - pc.x), Math.abs(s.cz - pc.z)) > range || s.total() < ci.threshold() || s.total() == 0) continue;
            Component.Group dom = s.dominant();
            int color = dom == null ? 0xFFFFFFFF : ci.groupColor(dom);
            double x0 = s.cx * 16.0, z0 = s.cz * 16.0;
            double y = Math.max(mc.world.getBottomY(), s.topY == Integer.MIN_VALUE ? mc.player.getY() : s.topY);
            if (borders.get()) {
                Box b = new Box(x0, y - 1, z0, x0 + 16, y + 0.05, z0 + 16);
                RenderManager.boxOutline(b, ColorUtil.multiplyAlpha(color, pulse * alpha), ci.outlineThickness.getFloat(), xr);
            }
            if (fill.get()) RenderManager.flatQuad(x0, z0, x0 + 16, z0 + 16, y + 0.06, ColorUtil.withAlpha(ci.heatColor(s.total()), Math.round(60 * alpha)), xr);
            if (panels.get()) {
                Vec3d at = new Vec3d(x0 + 8, y + panelHeight.get(), z0 + 8);
                double dist = at.distanceTo(mc.player.getEyePos());
                MutableText t = Text.literal("CHUNK " + s.cx + " / " + s.cz).styled(st -> st.withColor(color & 0xFFFFFF).withBold(true));
                if (distanceLabels.get()) t.append(Text.literal(String.format(Locale.ROOT, "  %.0fm", dist)).styled(st -> st.withColor(0xAAAAAA)));
                RenderManager.text(t, at, 0.3f, 0xFFFFFFFF, ColorUtil.withAlpha(0, Math.round(170 * alpha)), xr, true);
                int shown = 0;
                double lineH = 10 * 0.26 * Math.max(1, dist) * 0.1;
                for (Component c : Component.values()) {
                    int v = s.get(c);
                    if (v <= 0) continue;
                    if (shown++ >= rows.getInt()) break;
                    int gc = ci.groupColor(c.group);
                    RenderManager.text(Text.literal("■ ").styled(st -> st.withColor(gc & 0xFFFFFF)).append(Text.literal(c.label + ": " + v).styled(st -> st.withColor(0xFFFFFF))),
                            at.add(0, -lineH * shown, 0), 0.26f, 0xFFFFFFFF, ColorUtil.withAlpha(0, Math.round(140 * alpha)), xr, true);
                }
            }
        }
    }
}

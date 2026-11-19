package voidclient.modules.basehunting;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ChunkPos;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Scans chunks the server has sent you and counts redstone, mechanisms, storage and farm
 * components. Numbers reflect client-visible data only (servers with anti-xray may hide
 * blocks); nothing is guessed for unloaded chunks.
 */
public class ChunkIntelligence extends HudModule {
    public static ChunkIntelligence INSTANCE;

    public final NumberSetting scanRadius = add(new NumberSetting("Scan Radius", "Chunks around you to inspect.", 4, 0, 12, 1).unit("ch"));
    public final NumberSetting updateInterval = add(new NumberSetting("Update Interval", "Ticks between full rescans.", 100, 20, 1200, 20));
    public final NumberSetting budget = add(new NumberSetting("Scan Budget", "Milliseconds of scanning per tick.", 2, 0.5, 8, 0.5).unit("ms"));
    public final NumberSetting displayDistance = add(new NumberSetting("Display Distance", "Chunk radius shown by Chunk Inspection ESP.", 4, 0, 12, 1).unit("ch"));
    public final MultiSelectSetting categories = add(new MultiSelectSetting("Inspection Categories", "Component groups counted.",
            List.of("Redstone", "Mechanisms", "Storage", "Farm Components"), Set.of("Redstone", "Mechanisms", "Storage", "Farm Components")));
    public final NumberSetting minCount = add(new NumberSetting("Minimum Count", "Ignore chunks below this total (0 = use profile minimum).", 0, 0, 500, 1));
    public final NumberSetting maxCount = add(new NumberSetting("Maximum Count", "Total that counts as maximum activity (heat scale).", 200, 10, 2000, 10));
    public final NumberSetting textSize = add(new NumberSetting("Text Size", "Panel text scale.", 1, 0.6, 2, 0.05));
    public final ModeSetting colorPreset = add(new ModeSetting("Color Preset", "Colour scheme for categories and heat.", "Neon", "Neon", "Heat", "Mono"));
    public final NumberSetting outlineThickness = add(new NumberSetting("Outline Thickness", "Chunk outline width for the ESP.", 2, 0.5, 6, 0.5));
    public final NumberSetting transparency = add(new NumberSetting("Transparency", "Panel and fill transparency.", 0.35, 0, 0.9, 0.05));
    public final BooleanSetting recordHistory = add(new BooleanSetting("Record History", "Save inspected chunks to Inspection History.", true));

    private final ChunkScanner scanner = new ChunkScanner();
    private int ticks;
    private int scannedTotal;

    public ChunkIntelligence() {
        super("Chunk Intelligence", "Inspects loaded chunks and counts redstone, mechanisms, storage and farm parts.", Category.BASE_HUNTING, 0.005, 0.35);
        INSTANCE = this;
        InspectionStore.init();
        on(Events.TickPost.class, e -> tick());
        on(Events.WorldLeave.class, e -> scanner.clear());
    }

    public ChunkScanner scanner() { return scanner; }

    private String dim() { return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString(); }

    public java.util.Set<Component> tracked() {
        var set = VoidClient.get().modules().get(InspectionProfiles.class).activeComponents();
        set.removeIf(c -> !categories.isSelected(c.group.label));
        return set;
    }

    public int threshold() {
        return minCount.getInt() > 0 ? minCount.getInt() : VoidClient.get().modules().get(InspectionProfiles.class).minimum();
    }

    private void tick() {
        if (mc.player == null || mc.world == null) return;
        ChunkPos c = mc.player.getChunkPos();
        if (ticks++ % updateInterval.getInt() == 0 || scanner.idle() && ticks % 40 == 0) scanner.schedule(c.x, c.z, scanRadius.getInt());
        for (ChunkStats s : scanner.tick(tracked(), dim(), (long) (budget.get() * 1_000_000))) {
            scannedTotal++;
            if (recordHistory.get() && s.total() >= threshold()) InspectionStore.record(s);
        }
    }

    public int groupColor(Component.Group g) {
        return switch (colorPreset.get()) {
            case "Heat" -> ColorUtil.lerp(0xFF3C8CFF, 0xFFFF3C3C, g.ordinal() / 3f);
            case "Mono" -> 0xFFE0E0F0;
            default -> g.color;
        };
    }

    public int heatColor(int total) {
        float f = Math.min(1f, total / maxCount.getFloat());
        return colorPreset.is("Mono") ? ColorUtil.lerp(0xFF808080, 0xFFFFFFFF, f) : ColorUtil.lerp(0xFF3CB4FF, 0xFFFF2BD6, f);
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        var m = ctx.getMatrices();
        float ts = textSize.getFloat();
        m.pushMatrix();
        m.scale(ts, ts);
        ChunkPos cp = mc.player == null ? new ChunkPos(0, 0) : mc.player.getChunkPos();
        ChunkStats s = scanner.get(cp.x, cp.z);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"CHUNK", cp.x + " / " + cp.z});
        if (s == null) rows.add(new String[]{"STATUS", mc.player == null ? "no world" : "scanning..."});
        else {
            for (Component.Group g : Component.Group.values()) {
                if (!categories.isSelected(g.label)) continue;
                rows.add(new String[]{g.label.toUpperCase(), String.valueOf(s.group(g)), String.valueOf(groupColor(g))});
            }
            for (Component c : Component.values()) {
                int v = s.get(c);
                if (v > 0) rows.add(new String[]{"  " + c.label, String.valueOf(v)});
            }
        }
        int nearby = 0, active = 0;
        for (ChunkStats st : scanner.results().values()) {
            nearby++;
            if (st.total() >= threshold()) active++;
        }
        rows.add(new String[]{"NEARBY", active + " active / " + nearby + " scanned"});
        float w = 0;
        for (String[] r : rows) w = Math.max(w, textWidth(r[0]) + textWidth(r[1]) + 14);
        float h = rows.size() * 10 + 16;
        size(w * ts, h * ts);
        panel(ctx, 0, 0, w, h);
        Render2D.rect(ctx, 4, 12, w - 8, 0.6f, ColorUtil.withAlpha(accent(), 160));
        text(ctx, "CHUNK INTELLIGENCE", 4, 3, accent());
        float y = 15;
        for (String[] r : rows) {
            int lc = r.length > 2 ? Integer.parseInt(r[2]) : dim(textColor.getColor());
            text(ctx, r[0], 4, y, lc);
            text(ctx, r[1], w - 4 - textWidth(r[1]), y, textColor.getColor());
            y += 10;
        }
        m.popMatrix();
    }

    @Override
    public String getInfo() { return scanner.results().size() + "ch"; }
}

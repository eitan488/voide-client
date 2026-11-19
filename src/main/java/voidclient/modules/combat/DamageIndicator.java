package voidclient.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Floating damage and heal numbers above entities (from server-synced health). */
public class DamageIndicator extends Module {
    private record Indicator(Vec3d pos, String text, int color, long born, double drift) {}

    public final ModeSetting colorMode = add(new ModeSetting("Color Mode", "How numbers are coloured.", "Severity", "Static", "Severity", "Theme"));
    public final ColorSetting damageColor = add(new ColorSetting("Damage Color", "Static damage colour.", 0xFFFF3C6E).visibleWhen(() -> colorMode.is("Static")));
    public final BooleanSetting heals = add(new BooleanSetting("Heal Numbers", "Show health gains too.", true));
    public final ColorSetting healColor = add(new ColorSetting("Heal Color", "Heal number colour.", 0xFF3CFFA0).visibleWhen(heals::get));
    public final NumberSetting scale = add(new NumberSetting("Scale", "Number size.", 1.2, 0.4, 3, 0.05));
    public final NumberSetting duration = add(new NumberSetting("Duration", "Lifetime of a number.", 1200, 300, 4000, 100).unit("ms"));
    public final NumberSetting rise = add(new NumberSetting("Rise Speed", "Upward drift.", 0.8, 0, 3, 0.05));
    public final BooleanSetting onlyPlayers = add(new BooleanSetting("Only Players", "Only show for players.", false));
    public final BooleanSetting decimals = add(new BooleanSetting("Decimals", "Show one decimal.", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Draw numbers through blocks.", false));
    public final NumberSetting max = add(new NumberSetting("Max Indicators", "Upper limit of numbers on screen.", 40, 5, 200, 5));

    private final List<Indicator> indicators = new ArrayList<>();
    private final Random random = new Random();

    public DamageIndicator() {
        super("Damage Indicator", "Floating damage numbers when entities lose or gain health.", Category.COMBAT);
        alsoListIn(Category.VISUALS);
        on(Events.HealthChange.class, e -> {
            if (e.entity() == mc.player) return;
            if (onlyPlayers.get() && !(e.entity() instanceof PlayerEntity)) return;
            float delta = e.newHealth() - e.oldHealth();
            if (delta > 0 && !heals.get()) return;
            if (Math.abs(delta) < 0.05f) return;
            String txt = (delta > 0 ? "+" : "-") + String.format(Locale.ROOT, decimals.get() ? "%.1f" : "%.0f", Math.abs(delta));
            int c;
            if (delta > 0) c = healColor.getColor();
            else c = switch (colorMode.get()) {
                case "Static" -> damageColor.getColor();
                case "Theme" -> voidclient.gui.themes.ThemeManager.accent(0);
                default -> ColorUtil.lerp(0xFFFFE066, 0xFFFF2040, Math.min(1f, -delta / 10f));
            };
            Vec3d at = e.entity().getEntityPos().add((random.nextDouble() - 0.5) * 0.6, e.entity().getHeight() * 0.8, (random.nextDouble() - 0.5) * 0.6);
            indicators.add(new Indicator(at, txt, c, System.currentTimeMillis(), (random.nextDouble() - 0.5) * 0.3));
            while (indicators.size() > max.getInt()) indicators.remove(0);
        });
        on(Events.WorldRender.class, e -> {
            long now = System.currentTimeMillis();
            indicators.removeIf(i -> now - i.born() > duration.get());
            for (Indicator i : indicators) {
                float t = (now - i.born()) / duration.getFloat();
                Vec3d p = i.pos().add(i.drift() * t, rise.get() * t, 0);
                float pop = t < 0.15f ? 1f + (0.15f - t) * 3f : 1f;
                RenderManager.text(Text.literal(i.text()), p, scale.getFloat() * 0.25f * pop, ColorUtil.multiplyAlpha(i.color(), 1f - t * t), 0, throughWalls.get(), true);
            }
        });
        on(Events.WorldLeave.class, e -> indicators.clear());
    }
}

package voidclient.modules.visuals;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Weather readout (rain/thunder strength) and beams marking recent lightning strikes. */
public class WeatherVisualizer extends TextHudModule {
    private record Strike(Vec3d pos, long time) {}

    public final BooleanSetting markLightning = add(new BooleanSetting("Mark Lightning", "Beam where lightning struck.", true));
    public final NumberSetting markSeconds = add(new NumberSetting("Mark Duration", "How long strikes stay marked.", 20, 2, 120, 1).unit("s"));
    public final ColorSetting beamColor = add(new ColorSetting("Beam Color", "Lightning marker colour.", 0xFFE6E0FF, false));

    private final List<Strike> strikes = new ArrayList<>();
    private final java.util.Set<Integer> seen = new java.util.HashSet<>();

    public WeatherVisualizer() {
        super("Weather Visualizer", "Rain and thunder strength plus lightning strike markers.", Category.VISUALS, 0.005, 0.32);
        on(Events.TickPost.class, e -> {
            if (mc.world == null) return;
            for (Entity ent : mc.world.getEntities()) {
                if (ent instanceof LightningEntity && seen.add(ent.getId())) strikes.add(new Strike(ent.getEntityPos(), System.currentTimeMillis()));
            }
            strikes.removeIf(s -> System.currentTimeMillis() - s.time() > markSeconds.get() * 1000);
            if (seen.size() > 200) seen.clear();
        });
        on(Events.WorldRender.class, e -> {
            if (!markLightning.get()) return;
            for (Strike s : strikes) {
                float a = 1f - (System.currentTimeMillis() - s.time()) / (markSeconds.getFloat() * 1000);
                RenderManager.beam(s.pos(), 96, 0.3, ColorUtil.multiplyAlpha(beamColor.getColor(), a * 0.7f), 0, true);
            }
        });
        on(Events.WorldLeave.class, e -> {
            strikes.clear();
            seen.clear();
        });
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.world == null) {
            if (editor) out.add(new Line("Rain", "0%"));
            return;
        }
        float rain = mc.world.getRainGradient(1f), thunder = mc.world.getThunderGradient(1f);
        out.add(new Line("Rain", String.format(Locale.ROOT, "%.0f%%", rain * 100)));
        out.add(new Line("Thunder", String.format(Locale.ROOT, "%.0f%%", thunder * 100)));
        out.add(new Line("Strikes", String.valueOf(strikes.size())));
    }
}

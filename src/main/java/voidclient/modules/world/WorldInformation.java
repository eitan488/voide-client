package voidclient.modules.world;

import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;

import java.util.List;
import java.util.Locale;

/** World/server name, dimension, day, time, weather and difficulty. */
public class WorldInformation extends TextHudModule {
    public final BooleanSetting server = add(new BooleanSetting("World / Server", "World name or server address.", true));
    public final BooleanSetting dimension = add(new BooleanSetting("Dimension", "Current dimension.", true));
    public final BooleanSetting day = add(new BooleanSetting("Day", "Day count and in-game clock.", true));
    public final BooleanSetting weather = add(new BooleanSetting("Weather", "Clear, rain or thunder.", true));
    public final BooleanSetting difficulty = add(new BooleanSetting("Difficulty", "World difficulty.", false));

    public WorldInformation() {
        super("World Information", "World, dimension, day/time, weather and difficulty.", Category.WORLD, 0.005, 0.25);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.world == null) {
            if (editor) out.add(new Line("World", "Survival"));
            return;
        }
        if (server.get()) {
            String name = mc.isInSingleplayer() && mc.getServer() != null ? mc.getServer().getSaveProperties().getLevelName()
                    : mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Unknown";
            out.add(new Line(mc.isInSingleplayer() ? "World" : "Server", name));
        }
        if (dimension.get()) out.add(new Line("Dimension", mc.world.getRegistryKey().getValue().getPath().replace('_', ' ')));
        if (day.get()) {
            long t = mc.world.getTimeOfDay();
            long dayN = t / 24000;
            long tod = (t + 6000) % 24000;
            out.add(new Line("Day", dayN + "  " + String.format(Locale.ROOT, "%02d:%02d", tod / 1000, (tod % 1000) * 60 / 1000)));
        }
        if (weather.get()) out.add(new Line("Weather", mc.world.isThundering() ? "Thunder" : mc.world.isRaining() ? "Rain" : "Clear"));
        if (difficulty.get()) out.add(new Line("Difficulty", mc.world.getDifficulty().getName()));
    }
}

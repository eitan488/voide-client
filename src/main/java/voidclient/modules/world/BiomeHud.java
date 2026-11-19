package voidclient.modules.world;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;

import java.util.List;
import java.util.Locale;

/** Current biome name, id and temperature. */
public class BiomeHud extends TextHudModule {
    public final BooleanSetting showId = add(new BooleanSetting("Biome ID", "Show the registry id.", false));
    public final BooleanSetting temperature = add(new BooleanSetting("Temperature", "Show biome temperature.", false));

    public BiomeHud() {
        super("Biome HUD", "The biome you are standing in.", Category.WORLD, 0.005, 0.135);
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.world == null || mc.player == null) {
            if (editor) out.add(new Line("Biome", "Plains"));
            return;
        }
        RegistryEntry<Biome> b = mc.world.getBiome(mc.player.getBlockPos());
        String key = b.getKey().map(k -> k.getValue().toString()).orElse("unknown");
        String name = b.getKey().map(k -> net.minecraft.text.Text.translatable("biome." + k.getValue().getNamespace() + "." + k.getValue().getPath()).getString()).orElse(key);
        out.add(new Line("Biome", name));
        if (showId.get()) out.add(new Line("ID", key));
        if (temperature.get()) out.add(new Line("Temp", String.format(Locale.ROOT, "%.2f", b.value().getTemperature())));
    }
}

package voidclient.modules.render;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.MultiSelectSetting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Blocks chosen vanilla particle categories before they are created. */
public class ParticleControl extends Module {
    public static ParticleControl INSTANCE;

    private static final Map<ParticleType<?>, String> GROUPS = new HashMap<>();

    static {
        put("Explosions", ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, ParticleTypes.GUST, ParticleTypes.GUST_EMITTER_LARGE, ParticleTypes.GUST_EMITTER_SMALL);
        put("Block Break", ParticleTypes.BLOCK, ParticleTypes.DUST_PILLAR, ParticleTypes.FALLING_DUST, ParticleTypes.BLOCK_MARKER);
        put("Crits", ParticleTypes.CRIT, ParticleTypes.ENCHANTED_HIT, ParticleTypes.SWEEP_ATTACK);
        put("Damage", ParticleTypes.DAMAGE_INDICATOR);
        put("Smoke", ParticleTypes.SMOKE, ParticleTypes.LARGE_SMOKE, ParticleTypes.CAMPFIRE_COSY_SMOKE, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, ParticleTypes.WHITE_SMOKE);
        put("Totem", ParticleTypes.TOTEM_OF_UNDYING);
        put("Portal", ParticleTypes.PORTAL, ParticleTypes.REVERSE_PORTAL);
        put("Weather", ParticleTypes.RAIN, ParticleTypes.SPLASH, ParticleTypes.DRIPPING_WATER, ParticleTypes.FALLING_WATER);
        put("Potions", ParticleTypes.EFFECT, ParticleTypes.INSTANT_EFFECT, ParticleTypes.ENTITY_EFFECT, ParticleTypes.WITCH);
        put("Enchant", ParticleTypes.ENCHANT);
        put("Fire & Lava", ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.LAVA, ParticleTypes.DRIPPING_LAVA, ParticleTypes.SMALL_FLAME);
        put("Ambient", ParticleTypes.ASH, ParticleTypes.WHITE_ASH, ParticleTypes.CRIMSON_SPORE, ParticleTypes.WARPED_SPORE, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.MYCELIUM, ParticleTypes.UNDERWATER);
        put("Fireworks", ParticleTypes.FIREWORK, ParticleTypes.FLASH);
        put("Hearts & Villagers", ParticleTypes.HEART, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.ANGRY_VILLAGER);
    }

    private static void put(String group, ParticleType<?>... types) {
        for (ParticleType<?> t : types) GROUPS.put(t, group);
    }

    public final MultiSelectSetting blocked = add(new MultiSelectSetting("Blocked", "Particle categories to hide.",
            List.of("Explosions", "Block Break", "Crits", "Damage", "Smoke", "Totem", "Portal", "Weather", "Potions", "Enchant", "Fire & Lava", "Ambient", "Fireworks", "Hearts & Villagers"),
            Set.of("Explosions", "Weather")));

    public ParticleControl() {
        super("Particle Control", "Turn individual vanilla particle categories off.", Category.RENDER);
        INSTANCE = this;
    }

    public static boolean allow(ParticleEffect effect) {
        ParticleControl pc = INSTANCE;
        if (pc != null && pc.isEnabled()) {
            String g = GROUPS.get(effect.getType());
            if (g != null && pc.blocked.isSelected(g)) return false;
        }
        return CustomParticleQuality.allow(effect);
    }

    @Override
    public String getInfo() { return blocked.get().size() + " off"; }
}

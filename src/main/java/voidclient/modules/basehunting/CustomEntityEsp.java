package voidclient.modules.basehunting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import voidclient.core.setting.StringSetting;

import java.util.HashSet;
import java.util.Set;

/** ESP for exactly the entity types you list (e.g. villagers, wandering traders). */
public class CustomEntityEsp extends EntityEspBase {
    public final StringSetting types = add(new StringSetting("Entity Types", "Comma-separated entity ids.", "villager,wandering_trader,iron_golem", 600));
    private Set<EntityType<?>> cache = new HashSet<>();
    private String key = "";

    public CustomEntityEsp() {
        super("Custom Entity ESP", "ESP for a custom list of entity types.");
        color.defaultTo(new voidclient.core.setting.ColorSetting.ColorValue(0xFF00F0FF, false, false));
    }

    private Set<EntityType<?>> resolved() {
        if (!types.get().equals(key)) {
            key = types.get();
            cache = new HashSet<>();
            for (String s : key.split(",")) {
                if (s.isBlank()) continue;
                Identifier id = Identifier.tryParse(s.contains(":") ? s.trim() : "minecraft:" + s.trim());
                if (id != null && Registries.ENTITY_TYPE.containsId(id)) cache.add(Registries.ENTITY_TYPE.get(id));
            }
        }
        return cache;
    }

    @Override
    protected boolean accepts(Entity e) { return resolved().contains(e.getType()); }
}

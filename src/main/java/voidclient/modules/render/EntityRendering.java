package voidclient.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.utilities.FriendStore;

import java.util.List;
import java.util.Set;

/** Hide entity types and limit entity render distance for clarity and FPS. */
public class EntityRendering extends Module {
    public static EntityRendering INSTANCE;

    public final MultiSelectSetting hidden = add(new MultiSelectSetting("Hidden Types", "Entity types not rendered.",
            List.of("Armor Stands", "Item Frames", "Paintings", "Dropped Items", "XP Orbs", "Arrows", "Falling Blocks", "Fireworks", "Vehicles", "Players"),
            Set.of("Falling Blocks")));
    public final BooleanSetting keepFriends = add(new BooleanSetting("Keep Friends", "Friends stay visible when Players are hidden.", true));
    public final NumberSetting maxDistance = add(new NumberSetting("Entity Distance", "Max render distance for non-player entities (0 = vanilla).", 0, 0, 256, 4).unit("m"));
    public final NumberSetting playerDistance = add(new NumberSetting("Player Distance", "Max render distance for players (0 = vanilla).", 0, 0, 512, 8).unit("m"));

    public EntityRendering() {
        super("Entity Rendering", "Hide entity types and cap entity render distance.", Category.RENDER);
        INSTANCE = this;
    }

    private String type(Entity e) {
        if (e instanceof ArmorStandEntity) return "Armor Stands";
        if (e instanceof ItemFrameEntity) return "Item Frames";
        if (e instanceof PaintingEntity) return "Paintings";
        if (e instanceof ItemEntity) return "Dropped Items";
        if (e instanceof ExperienceOrbEntity) return "XP Orbs";
        if (e instanceof PersistentProjectileEntity) return "Arrows";
        if (e instanceof FallingBlockEntity) return "Falling Blocks";
        if (e instanceof FireworkRocketEntity) return "Fireworks";
        if (e instanceof VehicleEntity) return "Vehicles";
        if (e instanceof PlayerEntity) return "Players";
        return null;
    }

    public static boolean shouldRender(Entity e) {
        EntityRendering r = INSTANCE;
        if (r == null || !r.isEnabled()) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (e == mc.player || mc.player == null) return true;
        String t = r.type(e);
        if (t != null && r.hidden.isSelected(t)) {
            if (!(t.equals("Players") && r.keepFriends.get() && e instanceof PlayerEntity p && FriendStore.isFriend(p.getGameProfile().name()))) return false;
        }
        double max = e instanceof PlayerEntity ? r.playerDistance.get() : r.maxDistance.get();
        return max <= 0 || e.squaredDistanceTo(mc.player) <= max * max;
    }
}

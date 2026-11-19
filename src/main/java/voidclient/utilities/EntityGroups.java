package voidclient.utilities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.WaterAnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.VehicleEntity;

import java.util.List;

/** Coarse entity classification shared by ESP, highlight, counters and filters. */
public final class EntityGroups {
    public static final String PLAYERS = "Players", HOSTILE = "Hostile", NEUTRAL = "Neutral", PASSIVE = "Passive",
            ITEMS = "Items", PROJECTILES = "Projectiles", VEHICLES = "Vehicles", OTHER = "Other";
    public static final List<String> ALL = List.of(PLAYERS, HOSTILE, NEUTRAL, PASSIVE, ITEMS, PROJECTILES, VEHICLES, OTHER);

    private EntityGroups() {}

    public static String of(Entity e) {
        if (e instanceof PlayerEntity) return PLAYERS;
        if (e instanceof ItemEntity || e instanceof ExperienceOrbEntity) return ITEMS;
        if (e instanceof ProjectileEntity) return PROJECTILES;
        if (e instanceof VehicleEntity) return VEHICLES;
        if (e instanceof Angerable) return NEUTRAL;
        if (e instanceof Monster) return HOSTILE;
        if (e instanceof PassiveEntity || e instanceof WaterAnimalEntity) return PASSIVE;
        if (e instanceof LivingEntity && !(e instanceof ArmorStandEntity)) return PASSIVE;
        return OTHER;
    }
}

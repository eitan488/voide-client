package voidclient.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import voidclient.VoidClient;
import voidclient.events.Events;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared combat bookkeeping derived from data the server sends to the client:
 * health deltas of tracked living entities, totem pops (entity status 35), explosions,
 * last attacked target and time since last combat action.
 */
public final class CombatTracker {
    private final Map<Integer, Float> lastHealth = new HashMap<>();
    private final Map<Integer, Integer> pops = new HashMap<>();
    private LivingEntity lastTarget;
    private long lastAttackMs, lastDamageTakenMs, lastCombatMs;
    private int ticks;

    public void init() {
        var bus = VoidClient.get().events();
        bus.subscribe(Events.TickPost.class, e -> tick());
        bus.subscribe(Events.AttackEntity.class, e -> {
            if (e.target() instanceof LivingEntity le) lastTarget = le;
            lastAttackMs = lastCombatMs = System.currentTimeMillis();
        });
        bus.subscribe(Events.PacketReceive.class, e -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null) return;
            if (e.packet instanceof EntityStatusS2CPacket p && p.getStatus() == 35) {
                Entity ent = p.getEntity(mc.world);
                if (ent != null) {
                    int n = pops.merge(ent.getId(), 1, Integer::sum);
                    VoidClient.get().events().post(new Events.TotemPop(ent, n));
                }
            } else if (e.packet instanceof ExplosionS2CPacket p) {
                VoidClient.get().events().post(new Events.Explosion(p.center(), p.radius()));
            }
        });
        bus.subscribe(Events.WorldLeave.class, e -> reset());
    }

    private void reset() {
        lastHealth.clear();
        pops.clear();
        lastTarget = null;
    }

    private void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        if (++ticks % 200 == 0) lastHealth.keySet().removeIf(id -> mc.world.getEntityById(id) == null);
        if (lastTarget != null && (lastTarget.isRemoved() || lastTarget.distanceTo(mc.player) > 64)) lastTarget = null;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le)) continue;
            float hp = le.getHealth();
            if (!Float.isFinite(hp)) continue;
            Float prev = lastHealth.put(le.getId(), hp);
            if (prev != null && Math.abs(prev - hp) > 0.01f) {
                if (le == mc.player && hp < prev) lastDamageTakenMs = lastCombatMs = System.currentTimeMillis();
                VoidClient.get().events().post(new Events.HealthChange(le, prev, hp));
            }
            if (le.isDead() || le.getHealth() <= 0) pops.remove(le.getId());
        }
    }

    public LivingEntity lastTarget() { return lastTarget; }
    public long sinceAttackMs() { return System.currentTimeMillis() - lastAttackMs; }
    public long sinceDamageMs() { return System.currentTimeMillis() - lastDamageTakenMs; }
    public long sinceCombatMs() { return System.currentTimeMillis() - lastCombatMs; }
    public boolean everInCombat() { return lastCombatMs > 0; }
    public int pops(Entity e) { return pops.getOrDefault(e.getId(), 0); }
}

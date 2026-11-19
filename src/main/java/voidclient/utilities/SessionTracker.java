package voidclient.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import voidclient.VoidClient;
import voidclient.events.Events;

/** Session-wide statistics shared by the Session, Step and Combat HUDs. */
public final class SessionTracker {
    private long sessionStart = System.currentTimeMillis();
    private long worldJoin;
    private double walked, sprinted, swum, flown, elytra, ridden;
    private int jumps, attacks, blocksMined, deaths, totemPops;
    private Vec3d lastPos;
    private boolean wasOnGround = true;

    public void init() {
        var bus = VoidClient.get().events();
        bus.subscribe(Events.TickPost.class, e -> tick());
        bus.subscribe(Events.AttackEntity.class, e -> attacks++);
        bus.subscribe(Events.AttackBlock.class, e -> blocksMined++);
        bus.subscribe(Events.WorldJoin.class, e -> {
            worldJoin = System.currentTimeMillis();
            lastPos = null;
        });
        bus.subscribe(Events.TotemPop.class, e -> {
            if (e.entity() == MinecraftClient.getInstance().player) totemPops++;
        });
        bus.subscribe(Events.Death.class, e -> deaths++);
    }

    private void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null) {
            lastPos = null;
            return;
        }
        Vec3d pos = p.getEntityPos();
        if (lastPos != null) {
            double d = pos.distanceTo(lastPos);
            if (d < 20) {
                double horizontal = Math.hypot(pos.x - lastPos.x, pos.z - lastPos.z);
                if (p.hasVehicle()) ridden += d;
                else if (p.isGliding()) elytra += d;
                else if (p.getAbilities().flying) flown += d;
                else if (p.isTouchingWater()) swum += d;
                else if (p.isSprinting()) sprinted += horizontal;
                else walked += horizontal;
            }
        }
        if (wasOnGround && !p.isOnGround() && p.getVelocity().y > 0.2 && !p.getAbilities().flying && !p.hasVehicle()) jumps++;
        wasOnGround = p.isOnGround();
        lastPos = pos;
    }

    public long sessionMs() { return System.currentTimeMillis() - sessionStart; }
    public long worldMs() { return worldJoin == 0 ? 0 : System.currentTimeMillis() - worldJoin; }
    public double walked() { return walked; }
    public double sprinted() { return sprinted; }
    public double swum() { return swum; }
    public double flown() { return flown; }
    public double elytra() { return elytra; }
    public double ridden() { return ridden; }
    public double totalDistance() { return walked + sprinted + swum + flown + elytra + ridden; }
    public int jumps() { return jumps; }
    public int attacks() { return attacks; }
    public int blocksMined() { return blocksMined; }
    public int deaths() { return deaths; }
    public int totemPops() { return totemPops; }

    public void resetSession() {
        sessionStart = System.currentTimeMillis();
        walked = sprinted = swum = flown = elytra = ridden = 0;
        jumps = attacks = blocksMined = deaths = totemPops = 0;
    }
}

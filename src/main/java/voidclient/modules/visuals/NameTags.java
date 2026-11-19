package voidclient.modules.visuals;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
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
import voidclient.utilities.FriendStore;
import voidclient.utilities.InventoryUtil;

import java.util.Locale;

/** Custom name tags with health, distance, ping, armour durability and friend colouring. */
public class NameTags extends Module {
    public static NameTags INSTANCE;

    public final BooleanSetting players = add(new BooleanSetting("Players", "Draw tags for players.", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", "Draw tags for named or nearby mobs.", false));
    public final ModeSetting health = add(new ModeSetting("Health", "Health display style.", "Number", "None", "Number", "Hearts", "Percent"));
    public final BooleanSetting distance = add(new BooleanSetting("Distance", "Show distance in metres.", true));
    public final BooleanSetting ping = add(new BooleanSetting("Ping", "Show player latency.", true));
    public final BooleanSetting armor = add(new BooleanSetting("Armor", "Show the lowest armour durability.", false));
    public final BooleanSetting pops = add(new BooleanSetting("Totem Pops", "Show totem pops counted this session.", true));
    public final NumberSetting scale = add(new NumberSetting("Scale", "Tag size.", 1, 0.4, 3, 0.05));
    public final ColorSetting background = add(new ColorSetting("Background", "Tag background.", 0x99000000));
    public final ColorSetting friendColor = add(new ColorSetting("Friend Color", "Name colour of friends.", 0xFF3CFFA0));
    public final ColorSetting nameColor = add(new ColorSetting("Name Color", "Name colour of other players.", 0xFFFFFFFF));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "Draw tags through blocks.", true));
    public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Maximum tag distance.", 128, 8, 512, 8).unit("m"));

    public NameTags() {
        super("Name Tags", "Readable custom name tags with health, distance and ping.", Category.VISUALS);
        INSTANCE = this;
        on(Events.WorldRender.class, this::render);
    }

    public static boolean replacesVanilla(Entity e) {
        NameTags n = INSTANCE;
        if (n == null || !n.isEnabled()) return false;
        return e instanceof PlayerEntity ? n.players.get() : n.mobs.get() && e instanceof LivingEntity;
    }

    private void render(Events.WorldRender e) {
        if (!inWorld()) return;
        double max = maxDistance.get();
        for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof LivingEntity le) || ent == mc.player || !replacesVanilla(ent) || ent.isInvisibleTo(mc.player)) continue;
            if (!(ent instanceof PlayerEntity) && !ent.hasCustomName() && ent.distanceTo(mc.player) > 24) continue;
            double d = ent.distanceTo(mc.player);
            if (d > max) continue;
            Vec3d pos = ent.getLerpedPos(e.tickDelta).add(0, ent.getHeight() + 0.5, 0);
            RenderManager.text(build(le, d), pos, scale.getFloat() * 0.25f, 0xFFFFFFFF, background.getColor(), throughWalls.get(), true);
        }
    }

    private Text build(LivingEntity le, double d) {
        boolean friend = le instanceof PlayerEntity p && FriendStore.isFriend(p.getGameProfile().name());
        MutableText t = Text.empty();
        t.append(Text.literal(le.getName().getString()).styled(s -> s.withColor((friend ? friendColor.getColor() : nameColor.getColor()) & 0xFFFFFF)));
        float hp = le.getHealth() + le.getAbsorptionAmount();
        float frac = Math.min(1f, le.getHealth() / Math.max(1f, le.getMaxHealth()));
        int hc = ColorUtil.health(frac) & 0xFFFFFF;
        switch (health.get()) {
            case "Number" -> t.append(Text.literal(String.format(Locale.ROOT, " %.1f", hp)).styled(s -> s.withColor(hc)));
            case "Hearts" -> t.append(Text.literal(String.format(Locale.ROOT, " %.1f❤", hp / 2f)).styled(s -> s.withColor(hc)));
            case "Percent" -> t.append(Text.literal(" " + Math.round(frac * 100) + "%").styled(s -> s.withColor(hc)));
            default -> {
            }
        }
        if (distance.get()) t.append(Text.literal(String.format(Locale.ROOT, " %.0fm", d)).styled(s -> s.withColor(0xAAAAAA)));
        if (le instanceof PlayerEntity p) {
            if (ping.get() && mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (entry != null) t.append(Text.literal(" " + entry.getLatency() + "ms").styled(s -> s.withColor(0x7FDBFF)));
            }
            if (pops.get()) {
                int n = voidclient.VoidClient.get().combat().pops(p);
                if (n > 0) t.append(Text.literal(" -" + n).styled(s -> s.withColor(0xFFC93C)));
            }
            if (armor.get()) {
                float min = 1f;
                for (var slot : InventoryUtil.ARMOR) {
                    var st = p.getEquippedStack(slot);
                    if (!st.isEmpty() && st.isDamageable()) min = Math.min(min, InventoryUtil.durability(st));
                }
                int ac = ColorUtil.health(min) & 0xFFFFFF;
                t.append(Text.literal(" ⛨" + Math.round(min * 100) + "%").styled(s -> s.withColor(ac)));
            }
        }
        return t;
    }
}

package voidclient.modules.basehunting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import voidclient.core.setting.BooleanSetting;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.FriendStore;

import java.util.Locale;

/** Players (that the server sends you) as boxes/glow with health-coloured labels. */
public class PlayerEsp extends EntityEspBase {
    public final BooleanSetting healthColor = add(new BooleanSetting("Health Color", "Colour by health instead of the main colour.", false));
    public final BooleanSetting skipFriends = add(new BooleanSetting("Skip Friends", "Leave friends to Friend ESP.", true));

    public PlayerEsp() {
        super("Player ESP", "Highlights other players with boxes, glow and labels.");
        color.defaultTo(new voidclient.core.setting.ColorSetting.ColorValue(0xFFFF2BD6, false, false));
    }

    @Override
    protected boolean accepts(Entity e) {
        return e instanceof PlayerEntity p && !(skipFriends.get() && FriendStore.isFriend(p.getGameProfile().name()));
    }

    @Override
    protected int colorFor(Entity e) {
        if (healthColor.get() && e instanceof PlayerEntity p) return ColorUtil.health(p.getHealth() / Math.max(1, p.getMaxHealth()));
        return color.getColor();
    }

    @Override
    protected String label(Entity e) {
        PlayerEntity p = (PlayerEntity) e;
        return String.format(Locale.ROOT, "%s %.0f❤", p.getName().getString(), (p.getHealth() + p.getAbsorptionAmount()) / 2);
    }
}

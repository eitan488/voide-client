package voidclient.modules.basehunting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import voidclient.utilities.FriendStore;

/** Friends in their own colour so they stand out from everyone else. */
public class FriendEsp extends EntityEspBase {
    public FriendEsp() {
        super("Friend ESP", "Highlights players on your friend list.");
        color.defaultTo(new voidclient.core.setting.ColorSetting.ColorValue(0xFF3CFFA0, false, false));
        mode.defaultTo("Both");
    }

    @Override
    protected boolean accepts(Entity e) {
        return e instanceof PlayerEntity p && FriendStore.isFriend(p.getGameProfile().name());
    }
}

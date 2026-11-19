package voidclient.modules.utility;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.StringSetting;
import voidclient.events.Events;
import voidclient.gui.Notifications;
import voidclient.utilities.FriendStore;

/** Friends list used by ESP, name tags and filters. Middle-click a player to add/remove. */
public class FriendList extends Module {
    public final BooleanSetting middleClick = add(new BooleanSetting("Middle Click", "Middle-click a player to toggle friend.", true));
    public final StringSetting addName = add(new StringSetting("Add By Name", "Type a name and press Enter to add it.", "", 16));

    public FriendList() {
        super("Friend List", "Manage friends (also /void friend add <name>).", Category.UTILITY);
        on(Events.MouseButton.class, e -> {
            if (!middleClick.get() || e.button != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || e.action != GLFW.GLFW_PRESS || mc.currentScreen != null) return;
            if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity p) {
                String name = p.getGameProfile().name();
                if (FriendStore.isFriend(name)) {
                    FriendStore.remove(name);
                    Notifications.info("Friends", "Removed " + name);
                } else {
                    FriendStore.add(name);
                    Notifications.success("Friends", "Added " + name);
                }
            }
        });
        addName.onChange(v -> {
            if (v.endsWith(" ") || v.isBlank()) return;
        });
    }

    @Override
    protected void onEnable() {
        String n = addName.get().trim();
        if (!n.isEmpty() && FriendStore.add(n)) Notifications.success("Friends", "Added " + n);
    }

    @Override
    public String getInfo() { return String.valueOf(FriendStore.all().size()); }
}

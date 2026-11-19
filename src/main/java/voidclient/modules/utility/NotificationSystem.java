package voidclient.modules.utility;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.gui.Notifications;
import voidclient.utilities.Sounds;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Configures the animated toast notifications. Errors are always shown. */
public class NotificationSystem extends Module {
    public final NumberSetting duration = add(new NumberSetting("Duration", "How long each toast stays.", 2500, 800, 10000, 100).unit("ms"));
    public final NumberSetting scale = add(new NumberSetting("Scale", "Toast size.", 1, 0.6, 2, 0.05));
    public final BooleanSetting top = add(new BooleanSetting("Top Corner", "Show in the top-right instead of bottom-right.", false));
    public final MultiSelectSetting types = add(new MultiSelectSetting("Types", "Which kinds are shown.", List.of("Info", "Success", "Warning", "Error"), Set.of("Info", "Success", "Warning", "Error")));
    public final SoundSetting sound = add(new SoundSetting("Sound", "Played for each notification.", Sounds.ClientSound.NOTIFICATION, true, 0.35));

    public NotificationSystem() {
        super("Notification System", "Animated toast notifications with sounds.", Category.UTILITY);
        alsoListIn(Category.VISUALS);
    }

    public boolean accepts(Notifications.Type t) {
        if (t == Notifications.Type.ERROR) return true;
        if (!isEnabled()) return false;
        String n = t.name().charAt(0) + t.name().substring(1).toLowerCase(Locale.ROOT);
        return types.isSelected(n);
    }

    public long durationMs() { return duration.getInt(); }
    public float scale() { return scale.getFloat(); }
    public boolean topCorner() { return top.get(); }

    public void playSound(Notifications.Type t) {
        if (isEnabled()) sound.play();
    }
}

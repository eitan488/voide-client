package voidclient.modules.utility;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.events.Events;
import voidclient.gui.Notifications;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.MathUtil;
import voidclient.utilities.Sounds;

/** Reminds you (sound, toast, flash) after you have been idle for a while. It does not move you. */
public class AntiAfkReminder extends Module {
    public final NumberSetting idleMinutes = add(new NumberSetting("Idle Time", "Minutes without input before reminding.", 5, 1, 60, 1).unit("min"));
    public final NumberSetting repeat = add(new NumberSetting("Repeat Every", "Minutes between reminders.", 2, 1, 30, 1).unit("min"));
    public final SoundSetting sound = add(new SoundSetting("Sound", "Reminder sound.", Sounds.ClientSound.NOTIFICATION, true, 1));
    public final BooleanSetting flash = add(new BooleanSetting("Screen Flash", "Flash the screen edges.", true));

    private long lastInput = System.currentTimeMillis(), lastRemind;

    public AntiAfkReminder() {
        super("Anti AFK Reminder", "Alerts you when you've been idle too long.", Category.UTILITY);
        on(Events.Key.class, e -> lastInput = System.currentTimeMillis());
        on(Events.MouseButton.class, e -> lastInput = System.currentTimeMillis());
        on(Events.TickPost.class, e -> {
            long idle = System.currentTimeMillis() - lastInput;
            if (idle > idleMinutes.get() * 60_000 && System.currentTimeMillis() - lastRemind > repeat.get() * 60_000) {
                lastRemind = System.currentTimeMillis();
                sound.play();
                Notifications.warning("Still there?", "Idle for " + MathUtil.formatDuration(idle));
            }
        });
        on(Events.HudRender.class, e -> {
            if (!flash.get() || System.currentTimeMillis() - lastInput < idleMinutes.get() * 60_000) return;
            float p = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0));
            int c = ColorUtil.multiplyAlpha(0x60FFC93C, p);
            Render2D.gradientV(e.ctx, 0, 0, e.width, 20, c, 0);
            Render2D.gradientV(e.ctx, 0, e.height - 20, e.width, 20, 0, c);
        });
    }

    @Override
    protected void onEnable() { lastInput = System.currentTimeMillis(); }
}

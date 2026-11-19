package voidclient.modules.misc;

import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.gui.Notifications;
import voidclient.gui.screens.ClickGuiScreen;

/** Autosave behaviour and quick access to the profile manager. */
public class ConfigurationManager extends Module {
    public final NumberSetting interval = add(new NumberSetting("Autosave Interval", "Seconds between autosaves when settings changed.", 20, 5, 300, 5).unit("s"));
    public final BooleanSetting saveOnLeave = add(new BooleanSetting("Save On World Leave", "Save when you leave a world or server.", true));
    public final BooleanSetting notifyOnSave = add(new BooleanSetting("Notify On Save", "Toast after each automatic save.", false));
    public final BooleanSetting openProfiles = add(new BooleanSetting("Open Profiles Page", "Turn on to jump to the Profiles page.", false));

    public ConfigurationManager() {
        super("Configuration Manager", "Autosave timing and profile management shortcuts.", Category.MISC);
        interval.onChange(v -> VoidClient.get().clientSettings().autosaveSeconds.set(v));
        openProfiles.onChange(v -> {
            if (v) {
                openProfiles.set(false);
                mc.setScreen(new ClickGuiScreen(mc.currentScreen, Category.PROFILES));
            }
        });
        on(Events.WorldLeave.class, e -> {
            if (saveOnLeave.get()) {
                VoidClient.get().config().saveAll();
                if (notifyOnSave.get()) Notifications.info("Saved", VoidClient.get().profiles().active());
            }
        });
    }

    @Override
    public String getInfo() { return interval.getInt() + "s"; }
}

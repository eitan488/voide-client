package voidclient.modules.basehunting;

import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.gui.Notifications;

/** Storage policy for inspection history (browse it in the Chunk Browser). */
public class InspectionHistory extends Module {
    public final NumberSetting maxEntries = add(new NumberSetting("Max Entries", "History size per world (bookmarks are kept).", 5000, 100, 50000, 100));
    public final NumberSetting retentionDays = add(new NumberSetting("Retention", "Delete un-bookmarked entries older than this (0 = keep).", 0, 0, 365, 1).unit("d"));
    public final BooleanSetting clearNow = add(new BooleanSetting("Clear History", "Turn on to clear non-bookmarked history for this world.", false));

    public InspectionHistory() {
        super("Inspection History", "Retention and size of the chunk inspection history.", Category.BASE_HUNTING);
        maxEntries.onChange(v -> InspectionStore.setMaxEntries(v.intValue()));
        clearNow.onChange(v -> {
            if (v) {
                InspectionStore.clear();
                InspectionStore.save();
                clearNow.set(false);
                Notifications.info("Inspection History", "Cleared (bookmarks kept)");
            }
        });
        on(voidclient.events.Events.WorldJoin.class, e -> {
            if (retentionDays.get() > 0) InspectionStore.prune((long) (retentionDays.get() * 86_400_000L));
        });
    }

    @Override
    protected void onEnable() { InspectionStore.setMaxEntries(maxEntries.getInt()); }
}

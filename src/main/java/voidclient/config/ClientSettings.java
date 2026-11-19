package voidclient.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.Setting;
import voidclient.core.setting.SoundSetting;
import voidclient.utilities.Sounds;
import voidclient.utilities.Sounds.ClientSound;

import java.util.ArrayList;
import java.util.List;

/** Global (profile-independent) client preferences, stored in client.json. */
public final class ClientSettings {
    private final List<Setting<?>> settings = new ArrayList<>();

    public final NumberSetting guiScale = add(new NumberSetting("GUI Scale", "Size of the Void GUI relative to the screen.", 1.0, 0.6, 1.6, 0.05).unit("x"));
    public final BooleanSetting autoScale = add(new BooleanSetting("Auto Scale", "Keep the GUI the same physical size at every resolution (1080p - 4K).", true));
    public final BooleanSetting blurBackground = add(new BooleanSetting("Blur Background", "Blur the game behind the Void GUI.", true));
    public final BooleanSetting pauseSingleplayer = add(new BooleanSetting("Pause Singleplayer", "Pause singleplayer while the Void GUI is open.", false));
    public final BooleanSetting showDescriptions = add(new BooleanSetting("Card Descriptions", "Show module descriptions on cards.", true));
    public final BooleanSetting customMainMenu = add(new BooleanSetting("Custom Main Menu", "Replace the Minecraft title screen with the Void menu. Turn off to restore the original menu.", true));
    public final ModeSetting menuBackground = add(new ModeSetting("Menu Background", "Main menu background preset.", "Nebula", "Nebula", "Aurora", "Starfield", "Synthwave", "AMOLED"));
    public final NumberSetting menuParticles = add(new NumberSetting("Menu Particles", "Number of animated particles on the main menu.", 90, 0, 300, 5));
    public final BooleanSetting customServerList = add(new BooleanSetting("Custom Server List", "Use the Void server browser for Multiplayer.", true));
    public final BooleanSetting customLoadingScreen = add(new BooleanSetting("Custom Loading Screen", "Void-styled resource loading overlay.", true));
    public final NumberSetting masterVolume = add(new NumberSetting("UI Master Volume", "Multiplier for every Void UI sound.", 0.8, 0, 1, 0.05).onChange(v -> Sounds.masterVolume = v.floatValue()));
    public final SoundSetting openSound = add(new SoundSetting("Menu Open Sound", "Played when the GUI opens.", ClientSound.UI_OPEN, true, 0.7));
    public final SoundSetting closeSound = add(new SoundSetting("Menu Close Sound", "Played when the GUI closes.", ClientSound.UI_CLOSE, true, 0.6));
    public final SoundSetting clickSound = add(new SoundSetting("Click Sound", "Button clicks.", ClientSound.UI_CLICK, true, 0.5));
    public final SoundSetting moduleOnSound = add(new SoundSetting("Module On Sound", "Module activation.", ClientSound.MODULE_ON, true, 0.6));
    public final SoundSetting moduleOffSound = add(new SoundSetting("Module Off Sound", "Module deactivation.", ClientSound.MODULE_OFF, true, 0.6));
    public final SoundSetting categorySound = add(new SoundSetting("Category Sound", "Category transitions.", ClientSound.CATEGORY_SWITCH, true, 0.35));
    public final NumberSetting autosaveSeconds = add(new NumberSetting("Autosave Interval", "Seconds between automatic profile saves (only when something changed).", 20, 5, 300, 5).unit("s"));
    public final NumberSetting hudGrid = add(new NumberSetting("HUD Editor Grid", "Grid size used by snap-to-grid in the HUD editor.", 8, 2, 32, 1).unit("px"));
    public final BooleanSetting hudSnap = add(new BooleanSetting("HUD Snap To Grid", "Snap dragged HUD elements to the grid.", true));
    public final BooleanSetting hudGuides = add(new BooleanSetting("HUD Alignment Guides", "Show centre/edge alignment guides while dragging.", true));
    public final BooleanSetting hudAutoScale = add(new BooleanSetting("HUD Auto Scale",
            "Size HUD elements relative to the screen height so layouts look the same at every resolution and GUI scale (reference: 1080p at GUI scale 4).", true));

    private <S extends Setting<?>> S add(S s) {
        settings.add(s);
        return s;
    }

    public List<Setting<?>> all() { return settings; }

    public JsonObject save() {
        JsonObject o = new JsonObject();
        for (Setting<?> s : settings) o.add(s.getName(), s.toJson());
        return o;
    }

    public void load(JsonObject o) {
        if (o == null) return;
        for (Setting<?> s : settings) {
            JsonElement e = o.get(s.getName());
            if (e != null) s.fromJson(e);
        }
        Sounds.masterVolume = masterVolume.getFloat();
    }
}

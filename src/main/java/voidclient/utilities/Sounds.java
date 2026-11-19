package voidclient.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import voidclient.VoidClient;

/** Original client UI sounds (assets/voidclient/sounds, synthesized for this project). */
public final class Sounds {
    public enum ClientSound {
        UI_OPEN("ui_open"), UI_CLOSE("ui_close"), UI_CLICK("ui_click"),
        MODULE_ON("module_on"), MODULE_OFF("module_off"), NOTIFICATION("notification"),
        CATEGORY_SWITCH("category_switch"), TRAINING_HIT("training_hit"), TRAINING_MISS("training_miss"),
        WARNING("warning");

        public final SoundEvent event;

        ClientSound(String path) {
            this.event = SoundEvent.of(Identifier.of(VoidClient.MOD_ID, path));
        }
    }

    private Sounds() {}

    /** Master UI volume multiplier, set by the client settings. */
    public static volatile float masterVolume = 1f;

    public static void play(ClientSound sound, float volume, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        float v = volume * masterVolume;
        if (v <= 0.001f) return;
        mc.getSoundManager().play(PositionedSoundInstance.ui(sound.event, pitch, v));
    }
}

package voidclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import voidclient.utilities.Sounds;

/** Choice of a client sound plus its own enable flag, volume and pitch. */
public class SoundSetting extends Setting<SoundSetting.SoundValue> {
    public record SoundValue(Sounds.ClientSound sound, boolean enabled, double volume, double pitch) {}

    public SoundSetting(String name, String description, Sounds.ClientSound sound, boolean enabled, double volume) {
        super(name, description, new SoundValue(sound, enabled, volume, 1.0));
    }

    public void play() {
        SoundValue v = value;
        if (v.enabled()) Sounds.play(v.sound(), (float) v.volume(), (float) v.pitch());
    }

    public void setSound(Sounds.ClientSound s) { set(new SoundValue(s, value.enabled(), value.volume(), value.pitch())); }
    public void setEnabled(boolean e) { set(new SoundValue(value.sound(), e, value.volume(), value.pitch())); }
    public void setVolume(double vol) { set(new SoundValue(value.sound(), value.enabled(), vol, value.pitch())); }
    public void setPitch(double p) { set(new SoundValue(value.sound(), value.enabled(), value.volume(), p)); }

    @Override
    protected SoundValue sanitize(SoundValue v) {
        if (v == null || v.sound() == null) return null;
        double vol = Double.isNaN(v.volume()) ? 1 : Math.max(0, Math.min(1, v.volume()));
        double pitch = Double.isNaN(v.pitch()) ? 1 : Math.max(0.5, Math.min(2, v.pitch()));
        return new SoundValue(v.sound(), v.enabled(), vol, pitch);
    }

    @Override
    public JsonElement toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("sound", value.sound().name());
        o.addProperty("enabled", value.enabled());
        o.addProperty("volume", value.volume());
        o.addProperty("pitch", value.pitch());
        return o;
    }

    @Override
    public void fromJson(JsonElement e) {
        if (!(e instanceof JsonObject o)) return;
        try {
            Sounds.ClientSound s = o.has("sound") ? Sounds.ClientSound.valueOf(o.get("sound").getAsString()) : value.sound();
            boolean en = !o.has("enabled") || o.get("enabled").getAsBoolean();
            double vol = o.has("volume") ? o.get("volume").getAsDouble() : value.volume();
            double p = o.has("pitch") ? o.get("pitch").getAsDouble() : value.pitch();
            set(new SoundValue(s, en, vol, p));
        } catch (RuntimeException ignored) {
        }
    }
}

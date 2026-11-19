package voidclient.modules.training;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import voidclient.VoidClient;
import voidclient.config.JsonFiles;

import java.util.ArrayList;
import java.util.List;

/** Persistent training session history (config/voidclient/training_history.json). */
public final class TrainingHistory {
    public record Session(String mode, String scenario, String difficulty, long date, long durationMs, int score,
                          int placements, int detonations, int hits, float damage, float accuracy, long avgCycleMs, long bestCycleMs) {
        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("mode", mode);
            o.addProperty("scenario", scenario);
            o.addProperty("difficulty", difficulty);
            o.addProperty("date", date);
            o.addProperty("durationMs", durationMs);
            o.addProperty("score", score);
            o.addProperty("placements", placements);
            o.addProperty("detonations", detonations);
            o.addProperty("hits", hits);
            o.addProperty("damage", damage);
            o.addProperty("accuracy", accuracy);
            o.addProperty("avgCycleMs", avgCycleMs);
            o.addProperty("bestCycleMs", bestCycleMs);
            return o;
        }

        static Session fromJson(JsonObject o) {
            return new Session(o.get("mode").getAsString(), o.get("scenario").getAsString(), o.get("difficulty").getAsString(), o.get("date").getAsLong(),
                    o.get("durationMs").getAsLong(), o.get("score").getAsInt(), o.get("placements").getAsInt(), o.get("detonations").getAsInt(),
                    o.get("hits").getAsInt(), o.get("damage").getAsFloat(), o.get("accuracy").getAsFloat(), o.get("avgCycleMs").getAsLong(), o.get("bestCycleMs").getAsLong());
        }
    }

    private static final List<Session> SESSIONS = new ArrayList<>();
    private static boolean loaded;

    private TrainingHistory() {}

    private static void ensure() {
        if (loaded) return;
        loaded = true;
        JsonObject o = JsonFiles.read(VoidClient.get().config().file("training_history.json"));
        if (o != null && o.get("sessions") instanceof JsonArray a) {
            for (JsonElement e : a) {
                try {
                    SESSIONS.add(Session.fromJson(e.getAsJsonObject()));
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    public static List<Session> all() {
        ensure();
        return SESSIONS;
    }

    public static List<Session> of(String mode) {
        List<Session> out = new ArrayList<>();
        for (Session s : all()) if (s.mode().equals(mode)) out.add(s);
        return out;
    }

    public static Session best(String mode) {
        Session best = null;
        for (Session s : of(mode)) if (best == null || s.score() > best.score()) best = s;
        return best;
    }

    public static void add(Session s) {
        ensure();
        SESSIONS.add(s);
        while (SESSIONS.size() > 500) SESSIONS.remove(0);
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        for (Session x : SESSIONS) a.add(x.toJson());
        o.add("sessions", a);
        JsonFiles.write(VoidClient.get().config().file("training_history.json"), o);
    }
}

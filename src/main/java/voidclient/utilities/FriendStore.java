package voidclient.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import voidclient.VoidClient;
import voidclient.config.JsonFiles;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Persistent friend list (config/voidclient/friends.json), case-insensitive. */
public final class FriendStore {
    private static final Set<String> FRIENDS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private static boolean loaded;

    private FriendStore() {}

    private static void ensure() {
        if (loaded) return;
        loaded = true;
        JsonObject o = JsonFiles.read(VoidClient.get().config().file("friends.json"));
        if (o != null && o.get("friends") instanceof JsonArray a) for (JsonElement e : a) FRIENDS.add(e.getAsString());
    }

    public static boolean isFriend(String name) {
        ensure();
        return name != null && FRIENDS.contains(name);
    }

    public static boolean add(String name) {
        ensure();
        boolean added = FRIENDS.add(name.trim());
        save();
        return added;
    }

    public static boolean remove(String name) {
        ensure();
        boolean removed = FRIENDS.remove(name.trim());
        save();
        return removed;
    }

    public static Set<String> all() {
        ensure();
        return Set.copyOf(FRIENDS);
    }

    private static void save() {
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        FRIENDS.forEach(a::add);
        o.add("friends", a);
        JsonFiles.write(VoidClient.get().config().file("friends.json"), o);
    }

    public static String normalize(String s) { return s.toLowerCase(Locale.ROOT); }
}

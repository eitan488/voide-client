package voidclient.core;

/** Sidebar entries. Entries with {@code modules == false} are dedicated pages. */
public enum Category {
    DASHBOARD("Dashboard", "dashboard", false),
    COMBAT("Combat", "combat", true),
    CRYSTAL("Crystal PvP", "crystal", true),
    ANCHOR("Anchor PvP", "anchor", true),
    BASE_HUNTING("Base Hunting", "basehunting", true),
    MOVEMENT("Movement", "movement", true),
    PLAYER("Player", "player", true),
    VISUALS("Visuals", "visuals", true),
    WORLD("World", "world", true),
    RENDER("Render", "render", true),
    UTILITY("Utility", "utility", true),
    MISC("Miscellaneous", "misc", true),
    HUD_EDITOR("HUD Editor", "hud", false),
    PROFILES("Profiles", "profiles", false),
    SETTINGS("Settings", "settings", false);

    public final String displayName;
    public final String icon;
    public final boolean modules;

    Category(String displayName, String icon, boolean modules) {
        this.displayName = displayName;
        this.icon = icon;
        this.modules = modules;
    }
}

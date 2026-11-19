# Config examples

`profiles/` holds the eight built-in profiles exactly as Void Client writes them on first start
(`.minecraft/config/voidclient/profiles/`). They are sparse: listed modules are enabled, optionally
with setting overrides; every module that is not listed is disabled and keeps its default settings
when the profile is applied.

To import one, copy it into `.minecraft/config/voidclient/imports/` and use
Profiles → Import → *From imports/*, or copy it straight into the `profiles/` folder.

Once you use a profile, the client saves a full snapshot into its file: every module, every setting,
the HUD layout and keybinds.

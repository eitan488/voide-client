package voidclient.gui.screens.pages;

import net.minecraft.client.gui.DrawContext;
import voidclient.gui.components.Widget;

/** A full content page inside the ClickGUI (Dashboard, Profiles, Settings, HUD). */
public abstract class Page extends Widget {
    /** Tooltip text for whatever is hovered, or null. */
    public String tooltip() { return null; }

    @Override
    public abstract void render(DrawContext ctx, double mx, double my, float delta);
}

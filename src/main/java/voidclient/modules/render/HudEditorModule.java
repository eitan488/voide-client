package voidclient.modules.render;

import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.gui.screens.HudEditorScreen;

/** Opens the standalone HUD editor; holds its snapping preferences. */
public class HudEditorModule extends Module {
    public final BooleanSetting snap = add(new BooleanSetting("Snap To Grid", "Snap elements to the grid while dragging.", true));
    public final NumberSetting grid = add(new NumberSetting("Grid Size", "Grid size in GUI pixels.", 8, 2, 32, 1));
    public final BooleanSetting guides = add(new BooleanSetting("Alignment Guides", "Snap to and show screen/element guides.", true));

    public HudEditorModule() {
        super("HUD Editor", "Drag, resize, align and layer every HUD element.", Category.RENDER);
        alsoListIn(Category.MISC);
        snap.onChange(v -> VoidClient.get().clientSettings().hudSnap.set(v));
        grid.onChange(v -> VoidClient.get().clientSettings().hudGrid.set(v));
        guides.onChange(v -> VoidClient.get().clientSettings().hudGuides.set(v));
    }

    @Override
    public boolean isToggleable() { return false; }

    @Override
    protected void runAction() { mc.setScreen(new HudEditorScreen(mc.currentScreen)); }
}

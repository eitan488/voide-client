package voidclient.modules.visuals;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import voidclient.VoidClient;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ModeSetting;
import voidclient.gui.Notifications;

/** Clean glass textures via built-in resource packs shipped with the client. */
public class CustomGlass extends Module {
    private static final Identifier CLEAR = Identifier.of(VoidClient.MOD_ID, "clear_glass");
    private static final Identifier BORDERLESS = Identifier.of(VoidClient.MOD_ID, "borderless_glass");

    public final ModeSetting style = add(new ModeSetting("Style", "Glass texture style.", "Clear", "Clear", "Borderless"));

    public CustomGlass() {
        super("Custom Glass", "Clear or borderless glass textures (built-in resource packs).", Category.VISUALS);
        FabricLoader.getInstance().getModContainer(VoidClient.MOD_ID).ifPresent(c -> {
            ResourceManagerHelper.registerBuiltinResourcePack(CLEAR, c, Text.literal("Void Clear Glass"), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(BORDERLESS, c, Text.literal("Void Borderless Glass"), ResourcePackActivationType.NORMAL);
        });
        style.onChange(v -> {
            if (isEnabled()) apply(true);
        });
    }

    private void apply(boolean on) {
        if (mc.getResourcePackManager() == null || mc.options == null) return;
        var pm = mc.getResourcePackManager();
        boolean changed = pm.disable(CLEAR.toString()) | pm.disable(BORDERLESS.toString());
        if (on) changed |= pm.enable((style.is("Clear") ? CLEAR : BORDERLESS).toString());
        if (changed) {
            mc.options.refreshResourcePacks(pm);
            Notifications.info("Custom Glass", "Reloading resources...");
        }
    }

    @Override
    protected void onEnable() { apply(true); }

    @Override
    protected void onDisable() { apply(false); }

    @Override
    public String getInfo() { return style.get(); }
}

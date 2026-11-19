package voidclient.keybinds;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import voidclient.VoidClient;
import voidclient.core.Module;
import voidclient.events.Events;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes raw key and mouse events to module keybinds. Supports toggle and hold modes,
 * mouse buttons, and reports conflicts with other modules and vanilla controls.
 */
public final class KeybindManager {
    public void init() {
        var bus = VoidClient.get().events();
        bus.subscribe(Events.Key.class, e -> {
            if (e.action == GLFW.GLFW_REPEAT) return;
            handle(false, e.key, e.action == GLFW.GLFW_PRESS);
        });
        bus.subscribe(Events.MouseButton.class, e -> handle(true, e.button, e.action == GLFW.GLFW_PRESS));
    }

    private void handle(boolean mouse, int code, boolean pressed) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null || mc.getOverlay() != null) return;
        if (!mouse && code == GLFW.GLFW_KEY_UNKNOWN) return;
        for (Module m : VoidClient.get().modules().all()) {
            Keybind kb = m.keybind.get();
            if (!(mouse ? kb.matchesMouse(code) : kb.matchesKey(code))) continue;
            if (kb.hold() && m.isToggleable()) {
                if (pressed != m.isEnabled()) VoidClient.get().modules().userToggle(m, false);
            } else if (pressed) {
                VoidClient.get().modules().userToggle(m, true);
            }
        }
    }

    /** Every other module and vanilla control bound to the same input. */
    public List<String> conflicts(Module owner, Keybind kb) {
        List<String> out = new ArrayList<>();
        if (!kb.isBound()) return out;
        for (Module m : VoidClient.get().modules().all()) {
            if (m == owner) continue;
            Keybind o = m.keybind.get();
            if (o.isBound() && o.code() == kb.code() && o.mouse() == kb.mouse()) out.add(m.getName());
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            InputUtil.Key key = kb.mouse() ? InputUtil.Type.MOUSE.createFromCode(kb.code()) : InputUtil.Type.KEYSYM.createFromCode(kb.code());
            for (KeyBinding binding : mc.options.allKeys) {
                if (binding.getBoundKeyTranslationKey().equals(key.getTranslationKey())) {
                    out.add("Minecraft: " + net.minecraft.text.Text.translatable(binding.getId()).getString());
                }
            }
        }
        return out;
    }

    public void clearAll() {
        for (Module m : VoidClient.get().modules().all()) m.keybind.reset();
    }
}

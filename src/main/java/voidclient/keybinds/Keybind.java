package voidclient.keybinds;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Immutable key or mouse-button binding. {@code code == -1} means unbound.
 * {@code hold} = module is active only while the key is held.
 */
public record Keybind(int code, boolean mouse, boolean hold) {
    public static final Keybind NONE = new Keybind(-1, false, false);

    public static Keybind key(int glfwKey) { return new Keybind(glfwKey, false, false); }
    public static Keybind mouse(int button) { return new Keybind(button, true, false); }

    public boolean isBound() { return code >= 0; }

    public Keybind withHold(boolean h) { return new Keybind(code, mouse, h); }

    public boolean matchesKey(int key) { return isBound() && !mouse && code == key; }
    public boolean matchesMouse(int button) { return isBound() && mouse && code == button; }

    public String displayName() {
        if (!isBound()) return "None";
        if (mouse) {
            return switch (code) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "LMB";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "RMB";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MMB";
                default -> "Mouse " + (code + 1);
            };
        }
        String name = GLFW.glfwGetKeyName(code, 0);
        if (name != null && !name.isBlank()) return name.toUpperCase(Locale.ROOT);
        return InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
    }

    public String serialize() {
        if (!isBound()) return "none";
        return (mouse ? "mouse:" : "key:") + code + (hold ? ":hold" : "");
    }

    public static Keybind parse(String s) {
        if (s == null || s.equals("none")) return NONE;
        String[] parts = s.split(":");
        try {
            if (parts.length < 2) return NONE;
            int code = Integer.parseInt(parts[1]);
            boolean hold = parts.length > 2 && parts[2].equals("hold");
            if (code < 0 || code > GLFW.GLFW_KEY_LAST) return NONE;
            return new Keybind(code, parts[0].equals("mouse"), hold);
        } catch (NumberFormatException e) {
            return NONE;
        }
    }
}

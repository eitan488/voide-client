package voidclient.modules.misc;

import org.lwjgl.glfw.GLFW;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.events.Events;

import java.util.ArrayDeque;
import java.util.List;

/** Left/right clicks per second with session maximum. Always counts (Keystrokes reads it). */
public class CpsCounter extends TextHudModule {
    public static CpsCounter INSTANCE;

    public final ModeSetting mode = add(new ModeSetting("Buttons", "Which buttons to show.", "Both", "Left", "Right", "Both"));
    public final BooleanSetting showMax = add(new BooleanSetting("Show Max", "Highest CPS this session.", true));

    private final ArrayDeque<Long> leftClicks = new ArrayDeque<>(), rightClicks = new ArrayDeque<>();
    private int maxLeft, maxRight;

    public CpsCounter() {
        super("CPS Counter", "Clicks per second for both mouse buttons.", Category.MISC, 0.005, 0.35);
        alsoListIn(Category.COMBAT);
        INSTANCE = this;
        voidclient.VoidClient.get().events().subscribe(Events.MouseButton.class, e -> {
            if (e.action != GLFW.GLFW_PRESS || mc.currentScreen != null) return;
            if (e.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) leftClicks.add(System.currentTimeMillis());
            if (e.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) rightClicks.add(System.currentTimeMillis());
        });
    }

    private int count(ArrayDeque<Long> q) {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
        return q.size();
    }

    public int left() {
        int c = count(leftClicks);
        maxLeft = Math.max(maxLeft, c);
        return c;
    }

    public int right() {
        int c = count(rightClicks);
        maxRight = Math.max(maxRight, c);
        return c;
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        int l = left(), r = right();
        String v = switch (mode.get()) {
            case "Left" -> String.valueOf(l);
            case "Right" -> String.valueOf(r);
            default -> l + " | " + r;
        };
        out.add(new Line("CPS", v));
        if (showMax.get()) out.add(new Line("Max", mode.is("Right") ? String.valueOf(maxRight) : mode.is("Left") ? String.valueOf(maxLeft) : maxLeft + " | " + maxRight));
    }
}

package voidclient.core;

import net.minecraft.client.gui.DrawContext;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD element made of "label: value" lines. Subclasses fill {@link #lines} each frame and add
 * their own feature-specific settings.
 */
public abstract class TextHudModule extends HudModule {
    public record Line(String label, String value, int valueColor) {
        public Line(String label, String value) { this(label, value, 0); }
    }

    public final BooleanSetting showLabels;
    public final ColorSetting labelColor;
    public final ColorSetting valueColor;
    public final ModeSetting alignment;
    private final List<Line> lines = new ArrayList<>();

    protected TextHudModule(String name, String description, Category category, double x, double y) {
        super(name, description, category, x, y);
        showLabels = add(new BooleanSetting("Show Labels", "Show the label before each value.", true));
        labelColor = add(new ColorSetting("Label Color", "Colour of labels.", 0xFFB026FF).themed().visibleWhen(showLabels::get));
        valueColor = add(new ColorSetting("Value Color", "Default colour of values.", 0xFFF2F2FF));
        alignment = add(new ModeSetting("Alignment", "Text alignment inside the panel.", "Left", "Left", "Right", "Center"));
    }

    /** Fill {@code out}. {@code editor} is true in the HUD editor (use sample data if needed). */
    protected abstract void lines(List<Line> out, boolean editor);

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        lines.clear();
        lines(lines, editor);
        if (lines.isEmpty()) {
            if (editor) lines.add(new Line(getName(), "-"));
            else {
                size(0, 0);
                return;
            }
        }
        float pad = 4, lh = fontHeight() + 2;
        float maxW = 0;
        for (Line l : lines) maxW = Math.max(maxW, lineWidth(l));
        float w = maxW + pad * 2, h = lines.size() * lh + pad * 2 - 2;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        float y = pad;
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            float lw = lineWidth(l);
            float x = switch (alignment.get()) {
                case "Right" -> w - pad - lw;
                case "Center" -> (w - lw) / 2f;
                default -> pad;
            };
            if (showLabels.get() && l.label() != null && !l.label().isEmpty()) {
                x += text(ctx, l.label() + " ", x, y, labelColor.getColor(i * 0.05f));
            }
            text(ctx, l.value(), x, y, l.valueColor() != 0 ? l.valueColor() : valueColor.getColor(i * 0.05f));
            y += lh;
        }
    }

    private float lineWidth(Line l) {
        float w = textWidth(l.value());
        if (showLabels.get() && l.label() != null && !l.label().isEmpty()) w += textWidth(l.label() + " ");
        return w;
    }
}

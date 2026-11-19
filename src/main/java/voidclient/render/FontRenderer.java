package voidclient.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import voidclient.utilities.ColorUtil;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Text drawing with selectable font families and sub-pixel placement. */
public final class FontRenderer {
    public enum Family {
        MINECRAFT("Minecraft", null),
        INTER("Inter", Identifier.of("voidclient", "inter")),
        INTER_BOLD("Inter Bold", Identifier.of("voidclient", "inter_bold")),
        MONO("JetBrains Mono", Identifier.of("voidclient", "mono"));

        public final String displayName;
        final Style style;

        Family(String displayName, Identifier font) {
            this.displayName = displayName;
            this.style = font == null ? Style.EMPTY : Style.EMPTY.withFont(new StyleSpriteSource.Font(font));
        }
    }

    private static final int CACHE = 512;
    private static final Map<Family, Map<String, Text>> CACHE_MAP = new EnumMap<>(Family.class);

    private FontRenderer() {}

    private static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    public static Text text(String s, Family f) {
        Map<String, Text> m = CACHE_MAP.computeIfAbsent(f, k -> new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Text> e) { return size() > CACHE; }
        });
        Text t = m.get(s);
        if (t == null) {
            t = Text.literal(s).setStyle(f.style);
            m.put(s, t);
        }
        return t;
    }

    public static float draw(DrawContext ctx, String s, float x, float y, int color, Family f, boolean shadow) {
        if (s == null || s.isEmpty() || ColorUtil.alpha(color) < 4) return width(s, f);
        Text t = text(s, f);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        ctx.drawText(tr(), t, 0, 0, color, shadow);
        m.popMatrix();
        return tr().getWidth(t);
    }

    public static float drawText(DrawContext ctx, Text t, float x, float y, int color, boolean shadow) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        ctx.drawText(tr(), t, 0, 0, color, shadow);
        m.popMatrix();
        return tr().getWidth(t);
    }

    public static float drawScaled(DrawContext ctx, String s, float x, float y, float scale, int color, Family f, boolean shadow) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        float w = draw(ctx, s, 0, 0, color, f, shadow);
        m.popMatrix();
        return w * scale;
    }

    public static void drawCentered(DrawContext ctx, String s, float cx, float y, int color, Family f, boolean shadow) {
        draw(ctx, s, cx - width(s, f) / 2f, y, color, f, shadow);
    }

    /** Per-character gradient text (used for logos and headers). */
    public static float drawGradient(DrawContext ctx, String s, float x, float y, int c1, int c2, Family f, boolean shadow) {
        if (s == null || s.isEmpty()) return 0;
        MutableText out = Text.empty();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            int c = ColorUtil.lerp(c1, c2, n == 1 ? 0 : i / (float) (n - 1));
            out.append(Text.literal(String.valueOf(s.charAt(i))).setStyle(f.style.withColor(c & 0xFFFFFF)));
        }
        return drawText(ctx, out, x, y, 0xFFFFFFFF, shadow);
    }

    public static float width(String s, Family f) {
        if (s == null || s.isEmpty()) return 0;
        return tr().getWidth(text(s, f));
    }

    public static float height(Family f) { return tr().fontHeight; }

    public static String trim(String s, float maxWidth, Family f) {
        if (width(s, f) <= maxWidth) return s;
        String ell = "…";
        float ew = width(ell, f);
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (width(b.toString() + c, f) + ew > maxWidth) break;
            b.append(c);
        }
        return b + ell;
    }
}

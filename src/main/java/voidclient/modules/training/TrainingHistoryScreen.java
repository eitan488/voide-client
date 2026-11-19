package voidclient.modules.training;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import voidclient.gui.components.UI;
import voidclient.gui.screens.VoidScreen;
import voidclient.gui.themes.Theme;
import voidclient.gui.themes.ThemeManager;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.MathUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Session history table with a score trend graph. */
public class TrainingHistoryScreen extends VoidScreen {
    private final String mode;
    private float scroll;

    public TrainingHistoryScreen(Screen parent, String mode) {
        super(Text.literal("Training History"), parent, 480, 260);
        this.mode = mode;
    }

    @Override
    protected void layout() {}

    @Override
    protected void renderVoid(DrawContext ctx, double mx, double my, float delta) {
        Theme t = ThemeManager.get();
        List<TrainingHistory.Session> list = new ArrayList<>(TrainingHistory.of(mode));
        Collections.reverse(list);
        float x = 16, y = 14, w = vw - 32;
        UI.bold(ctx, mode.toUpperCase(Locale.ROOT) + " TRAINING HISTORY", x, y, ThemeManager.accent(0));
        UI.small(ctx, list.size() + " sessions  •  Esc to close", x, y + 11, t.textDim());
        float gy = y + 24, gh = 60;
        Render2D.roundRect(ctx, x, gy, w, gh, 6, ColorUtil.withAlpha(t.surface(), 200));
        int n = Math.min(40, list.size());
        int max = 1;
        for (int i = 0; i < n; i++) max = Math.max(max, list.get(i).score());
        float px = -1, py = -1;
        for (int i = n - 1; i >= 0; i--) {
            float sx = x + 8 + (w - 16) * (n - 1 - i) / Math.max(1, n - 1f);
            float sy = gy + gh - 8 - (gh - 16) * list.get(i).score() / (float) max;
            if (px >= 0) Render2D.line(ctx, px, py, sx, sy, 1.5f, ThemeManager.accent(0));
            Render2D.circle(ctx, sx, sy, 1.8f, 0xFFFFFFFF);
            px = sx;
            py = sy;
        }
        String[] head = {"Date", "Scenario", "Difficulty", "Time", "Score", "Hits/Det", "Accuracy", "Damage", "Cycle avg", "Best"};
        float[] col = {0, 0.17f, 0.33f, 0.44f, 0.52f, 0.60f, 0.70f, 0.79f, 0.87f, 0.95f};
        float ty = gy + gh + 8;
        for (int i = 0; i < head.length; i++) UI.small(ctx, head[i], x + w * col[i], ty, t.textDim());
        ty += 10;
        Render2D.scissor(ctx, x, ty, w, vh - ty - 10);
        float ry = ty - scroll;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        for (TrainingHistory.Session s : list) {
            if (ry > ty - 12 && ry < vh) {
                Render2D.rect(ctx, x, ry + 9.5f, w, 0.4f, ColorUtil.withAlpha(t.stroke(), 80));
                String[] v = {f.format(Instant.ofEpochMilli(s.date()).atZone(ZoneId.systemDefault())), s.scenario(), s.difficulty(), MathUtil.formatDuration(s.durationMs()),
                        String.valueOf(s.score()), s.hits() + "/" + s.detonations(), Math.round(s.accuracy() * 100) + "%", String.format(Locale.ROOT, "%.1f", s.damage()),
                        s.avgCycleMs() + "ms", s.bestCycleMs() + "ms"};
                for (int i = 0; i < v.length; i++) UI.small(ctx, UI.trim(v[i], w * 0.12f / 0.75f), x + w * col[i], ry, i == 4 ? ThemeManager.accent(0) : t.text());
            }
            ry += 11;
        }
        Render2D.endScissor(ctx);
        if (list.isEmpty()) UI.small(ctx, "No sessions yet - enable the trainer to start one.", x, ty + 4, t.textDim());
    }

    @Override
    protected boolean onScroll(double mx, double my, double amount) {
        scroll = Math.max(0, scroll - (float) amount * 22);
        return true;
    }
}

package voidclient.modules.training;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import voidclient.core.Category;
import voidclient.core.HudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.events.Events;
import voidclient.gui.Notifications;
import voidclient.render.Render2D;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.MathUtil;
import voidclient.utilities.Sounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared practice-session logic for the Crystal and Anchor trainers: countdown, duration,
 * difficulty, dummies, scoring, live HUD and saved history. The trainer never acts for you;
 * it only measures what you do.
 */
public abstract class TrainerBase extends HudModule {
    public final ModeSetting scenario;
    public final ModeSetting difficulty = add(new ModeSetting("Difficulty", "Dummy movement, armour and count.", "Medium", "Easy", "Medium", "Hard", "Insane"));
    public final NumberSetting duration = add(new NumberSetting("Training Duration", "Session length.", 60, 15, 600, 5).unit("s"));
    public final NumberSetting dummyDistance = add(new NumberSetting("Dummy Distance", "How far in front of you dummies spawn.", 4, 2, 10, 0.5).unit("m"));
    public final BooleanSetting buildArena = add(new BooleanSetting("Build Arena", "Turn on to build a practice platform at your feet (singleplayer; modifies your world).", false));
    public final NumberSetting arenaRadius = add(new NumberSetting("Arena Radius", "Platform radius.", 6, 3, 12, 1));
    public final BooleanSetting countdown = add(new BooleanSetting("Countdown", "3-second countdown before the session.", true));
    public final MultiSelectSetting statistics = add(new MultiSelectSetting("Statistics Display", "Statistics shown on the trainer HUD.",
            List.of("Score", "Accuracy", "Timing", "Damage", "Best Session"), Set.of("Score", "Accuracy", "Timing", "Damage", "Best Session")));
    public final SoundSetting hitSound = add(new SoundSetting("Hit Sound", "Played when a detonation damages a target.", Sounds.ClientSound.TRAINING_HIT, true, 0.7));
    public final SoundSetting missSound = add(new SoundSetting("Miss Sound", "Played when a detonation misses.", Sounds.ClientSound.TRAINING_MISS, true, 0.5));
    public final BooleanSetting openHistory = add(new BooleanSetting("Open History", "Turn on to open the session history.", false));

    protected final TrainingArena arena = new TrainingArena();
    protected long startMs, countdownEnd;
    protected int placements, detonations, hits;
    protected float damage;
    protected final List<Long> cycles = new ArrayList<>();
    protected long hitWindowEnd;
    protected boolean hitThisWindow, windowOpen;

    protected TrainerBase(String name, String description, Category category, String... scenarios) {
        super(name, description, category, 0.38, 0.09);
        scenario = add(new ModeSetting("Scenario", "Practice scenario.", scenarios[0], scenarios));
        buildArena.onChange(v -> {
            if (!v) return;
            buildArena.set(false);
            if (!TrainingArena.available()) Notifications.warning(getName(), "Arena building only works in singleplayer");
            else {
                arena.buildPlatform(arenaRadius.getInt(), bedrockArena());
                Notifications.success(getName(), "Arena built");
            }
        });
        openHistory.onChange(v -> {
            if (!v) return;
            openHistory.set(false);
            mc.setScreen(new TrainingHistoryScreen(mc.currentScreen, mode()));
        });
        on(Events.TickPost.class, e -> baseTick());
        on(Events.HealthChange.class, e -> {
            if (!running() || e.newHealth() >= e.oldHealth() || System.currentTimeMillis() > hitWindowEnd) return;
            LivingEntity t = e.entity();
            if (t == mc.player || !countsAsTarget(t)) return;
            damage += e.oldHealth() - e.newHealth();
            if (!hitThisWindow) {
                hitThisWindow = true;
                hits++;
                hitSound.play();
            }
        });
        on(Events.WorldLeave.class, e -> setEnabled(false));
    }

    protected abstract String mode();

    protected boolean bedrockArena() { return false; }

    /** Scenarios that use dummies. */
    protected abstract boolean usesDummies();

    protected boolean countsAsTarget(LivingEntity e) {
        return usesDummies() ? TrainingArena.isDummy(e) : e.distanceTo(mc.player) < 16;
    }

    protected int diff() { return difficulty.index(); }

    protected double dummySpeed() { return new double[]{0, 1.5, 3, 5}[diff()]; }

    protected int dummyCount() { return new int[]{1, 1, 2, 3}[diff()]; }

    protected int armorTier() { return new int[]{0, 1, 2, 3}[diff()]; }

    public boolean running() { return isEnabled() && System.currentTimeMillis() >= countdownEnd; }

    /** Opens a 450 ms window in which target damage counts as a hit for this detonation. */
    protected void detonated() {
        detonations++;
        if (windowOpen && !hitThisWindow) missSound.play();
        hitWindowEnd = System.currentTimeMillis() + 450;
        hitThisWindow = false;
        windowOpen = true;
    }

    @Override
    protected void onEnable() {
        placements = detonations = hits = 0;
        damage = 0;
        cycles.clear();
        hitWindowEnd = 0;
        windowOpen = false;
        countdownEnd = System.currentTimeMillis() + (countdown.get() ? 3000 : 0);
        startMs = countdownEnd;
        if (usesDummies()) {
            if (TrainingArena.available()) arena.spawnDummies(dummyCount(), dummyDistance.get(), armorTier());
            else Notifications.info(getName(), "Multiplayer: tracking-only mode (no dummies)");
        }
        onSessionStart();
    }

    @Override
    protected void onDisable() {
        arena.clearDummies();
        if (startMs > 0 && System.currentTimeMillis() > startMs + 3000) finish();
        startMs = 0;
    }

    protected void onSessionStart() {}

    protected abstract void sessionTick(long elapsedMs);

    private void baseTick() {
        if (!isEnabled() || mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now < countdownEnd) return;
        long elapsed = now - startMs;
        if (windowOpen && now > hitWindowEnd) {
            if (!hitThisWindow) missSound.play();
            windowOpen = false;
        }
        if (elapsed >= duration.get() * 1000) {
            setEnabled(false);
            return;
        }
        if (usesDummies() && TrainingArena.available()) {
            arena.moveDummies(dummySpeed(), elapsed);
            if (elapsed > 2000 && elapsed % 1000 < 50 && arena.aliveDummies() == 0) arena.spawnDummies(dummyCount(), dummyDistance.get(), armorTier());
        }
        sessionTick(elapsed);
    }

    protected long avgCycle() {
        if (cycles.isEmpty()) return 0;
        long t = 0;
        for (long c : cycles) t += c;
        return t / cycles.size();
    }

    protected long bestCycle() {
        long b = Long.MAX_VALUE;
        for (long c : cycles) b = Math.min(b, c);
        return cycles.isEmpty() ? 0 : b;
    }

    protected float accuracy() { return detonations == 0 ? 0 : Math.min(1f, hits / (float) detonations); }

    protected int score() {
        long avg = avgCycle();
        double speedBonus = cycles.isEmpty() ? 0 : Math.max(0, 900 - avg) * cycles.size() * 0.15;
        return (int) Math.round(damage * 10 + hits * 25 + speedBonus);
    }

    private void finish() {
        long dur = System.currentTimeMillis() - startMs;
        var s = new TrainingHistory.Session(mode(), scenario.get(), difficulty.get(), System.currentTimeMillis(), dur, score(), placements, detonations, hits,
                damage, accuracy(), avgCycle(), bestCycle());
        TrainingHistory.add(s);
        Notifications.success(getName() + " complete", "Score " + s.score() + "  •  " + Math.round(s.accuracy() * 100) + "% accuracy");
    }

    @Override
    protected void renderHud(DrawContext ctx, float delta, boolean editor) {
        List<String[]> rows = new ArrayList<>();
        long now = System.currentTimeMillis();
        if (isEnabled() && now < countdownEnd) {
            rows.add(new String[]{"STARTING IN", String.valueOf((countdownEnd - now) / 1000 + 1)});
        } else {
            long left = isEnabled() ? (long) (duration.get() * 1000) - (now - startMs) : 0;
            rows.add(new String[]{scenario.get().toUpperCase(Locale.ROOT), MathUtil.formatDuration(Math.max(0, left))});
            if (statistics.isSelected("Score")) rows.add(new String[]{"Score", String.valueOf(score())});
            rows.add(new String[]{placementLabel(), String.valueOf(placements)});
            rows.add(new String[]{"Detonations", String.valueOf(detonations)});
            if (statistics.isSelected("Accuracy")) rows.add(new String[]{"Accuracy", Math.round(accuracy() * 100) + "%  (" + hits + " hits)"});
            if (statistics.isSelected("Damage")) rows.add(new String[]{"Damage", String.format(Locale.ROOT, "%.1f", damage)});
            if (statistics.isSelected("Timing")) rows.add(new String[]{"Cycle avg/best", avgCycle() + " / " + bestCycle() + " ms"});
            if (statistics.isSelected("Best Session")) {
                var best = TrainingHistory.best(mode());
                rows.add(new String[]{"Best", best == null ? "-" : String.valueOf(best.score())});
            }
            extraRows(rows);
        }
        float w = 0;
        for (String[] r : rows) w = Math.max(w, textWidth(r[0]) + textWidth(r[1]) + 16);
        float h = rows.size() * 10 + 6;
        size(w, h);
        panel(ctx, 0, 0, w, h);
        float y = 3;
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            text(ctx, r[0], 4, y, i == 0 ? accent() : dim(textColor.getColor()));
            text(ctx, r[1], w - 4 - textWidth(r[1]), y, textColor.getColor());
            y += 10;
        }
        if (isEnabled() && now >= countdownEnd) {
            float p = (float) Math.min(1, (now - startMs) / (duration.get() * 1000));
            Render2D.rect(ctx, 2, h - 1.5f, (w - 4) * (1 - p), 1, ColorUtil.withAlpha(accent(), 220));
        }
    }

    protected String placementLabel() { return "Placements"; }

    protected void extraRows(List<String[]> rows) {}

    @Override
    public String getInfo() { return scenario.get(); }
}

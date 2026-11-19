package voidclient.modules.utility;

import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import voidclient.core.Category;
import voidclient.core.TextHudModule;
import voidclient.core.setting.BooleanSetting;
import voidclient.events.Events;

import java.util.List;
import java.util.Locale;

/** Latency with colour, jitter, and server TPS estimated from time-update packets. */
public class PingDisplay extends TextHudModule {
    public final BooleanSetting jitter = add(new BooleanSetting("Jitter", "Ping variation.", true));
    public final BooleanSetting tps = add(new BooleanSetting("Server TPS", "Estimated from world time packets (sent every 20 ticks).", true));

    private final int[] pings = new int[20];
    private int head, samples;
    private long lastTimePacket, lastWorldTime;
    private double tpsEstimate = 20;

    public PingDisplay() {
        super("Ping Display", "Latency, jitter and server TPS.", Category.UTILITY, 0.005, 0.14);
        on(Events.TickPost.class, e -> {
            if (mc.player == null || mc.getNetworkHandler() == null || mc.player.age % 20 != 0) return;
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry == null) return;
            pings[head] = entry.getLatency();
            head = (head + 1) % pings.length;
            samples = Math.min(samples + 1, pings.length);
        });
        on(Events.PacketReceive.class, e -> {
            if (!(e.packet instanceof WorldTimeUpdateS2CPacket p)) return;
            long now = System.currentTimeMillis();
            if (lastTimePacket > 0) {
                double dt = (now - lastTimePacket) / 1000.0;
                long ticks = p.time() - lastWorldTime;
                if (dt > 0.2 && ticks > 0) tpsEstimate = Math.min(20, tpsEstimate * 0.6 + ticks / dt * 0.4);
            }
            lastTimePacket = now;
            lastWorldTime = p.time();
        });
        on(Events.WorldLeave.class, e -> {
            lastTimePacket = 0;
            samples = 0;
        });
    }

    @Override
    protected void lines(List<Line> out, boolean editor) {
        if (mc.isInSingleplayer() || mc.getNetworkHandler() == null) {
            out.add(new Line("Ping", mc.isInSingleplayer() ? "local" : "-"));
        } else {
            int ping = samples == 0 ? 0 : pings[(head - 1 + pings.length) % pings.length];
            int c = ping < 80 ? 0xFF3CFFA0 : ping < 180 ? 0xFFFFC93C : 0xFFFF3C6E;
            out.add(new Line("Ping", ping + " ms", c));
            if (jitter.get() && samples > 1) {
                double mean = 0;
                for (int i = 0; i < samples; i++) mean += pings[i];
                mean /= samples;
                double var = 0;
                for (int i = 0; i < samples; i++) var += (pings[i] - mean) * (pings[i] - mean);
                out.add(new Line("Jitter", String.format(Locale.ROOT, "%.0f ms", Math.sqrt(var / samples))));
            }
        }
        if (tps.get() && lastTimePacket > 0) out.add(new Line("TPS", String.format(Locale.ROOT, "%.1f", tpsEstimate), tpsEstimate > 18 ? 0 : 0xFFFFC93C));
    }
}

package voidclient.modules.utility;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.core.setting.SoundSetting;
import voidclient.core.setting.StringSetting;
import voidclient.events.Events;
import voidclient.mixin.ChatHudAccessor;
import voidclient.utilities.Sounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Anti-spam stacking, longer history, keyword highlights and message filters. */
public class ChatCustomizer extends Module {
    public static ChatCustomizer INSTANCE;

    public final BooleanSetting antiSpam = add(new BooleanSetting("Anti Spam", "Stack identical consecutive messages as (xN).", true));
    public final NumberSetting spamWindow = add(new NumberSetting("Stack Window", "Max seconds between stacked duplicates.", 30, 2, 300, 1).unit("s").visibleWhen(antiSpam::get));
    public final NumberSetting history = add(new NumberSetting("History Size", "Chat lines kept (vanilla 100).", 400, 100, 2000, 50));
    public final BooleanSetting highlightName = add(new BooleanSetting("Highlight My Name", "Highlight messages that mention you.", true));
    public final StringSetting highlightWords = add(new StringSetting("Highlight Words", "Comma-separated words to highlight.", "", 300));
    public final ColorSetting highlightColor = add(new ColorSetting("Highlight Color", "Highlight background colour.", 0xFFB026FF, false));
    public final SoundSetting highlightSound = add(new SoundSetting("Highlight Sound", "Ping when highlighted.", Sounds.ClientSound.NOTIFICATION, true, 0.5));
    public final StringSetting hideWords = add(new StringSetting("Hide Words", "Comma-separated words; matching messages are hidden.", "", 300));

    private String lastRaw;
    private Text base;
    private int repeat;
    private long lastTime;

    public ChatCustomizer() {
        super("Chat Customizer", "Anti-spam, longer history, highlights and filters for chat.", Category.UTILITY);
        INSTANCE = this;
        on(Events.ChatReceive.class, 100, this::onChat);
    }

    public static int historySize(int original) {
        ChatCustomizer c = INSTANCE;
        return c != null && c.isEnabled() ? c.history.getInt() : original;
    }

    private List<String> words(String csv) {
        List<String> out = new ArrayList<>();
        for (String s : csv.split(",")) if (!s.isBlank()) out.add(s.trim().toLowerCase(Locale.ROOT));
        return out;
    }

    private void onChat(Events.ChatReceive e) {
        String raw = e.message.getString();
        String lower = raw.toLowerCase(Locale.ROOT);
        for (String w : words(hideWords.get())) {
            if (lower.contains(w)) {
                e.cancel();
                return;
            }
        }
        if (antiSpam.get() && mc.inGameHud != null) {
            long now = System.currentTimeMillis();
            List<ChatHudLine> msgs = ((ChatHudAccessor) mc.inGameHud.getChatHud()).voidclient$messages();
            if (raw.equals(lastRaw) && now - lastTime < spamWindow.get() * 1000 && !msgs.isEmpty()) {
                ChatHudLine top = msgs.get(0);
                if (base == null) base = top.content();
                repeat++;
                MutableText t = base.copy().append(Text.literal(" (x" + repeat + ")").styled(s -> s.withColor(0x8888AA)));
                msgs.set(0, new ChatHudLine(mc.inGameHud.getTicks(), t, top.signature(), top.indicator()));
                mc.inGameHud.getChatHud().reset();
                lastTime = now;
                e.cancel();
                return;
            }
            lastRaw = raw;
            base = null;
            repeat = 1;
            lastTime = now;
        }
        boolean hit = false;
        if (highlightName.get() && mc.getSession() != null) {
            String me = mc.getSession().getUsername().toLowerCase(Locale.ROOT);
            int sep = Math.max(lower.indexOf('>'), lower.indexOf(':'));
            if (lower.substring(Math.max(0, sep)).contains(me) && !lower.startsWith("<" + me + ">")) hit = true;
        }
        for (String w : words(highlightWords.get())) if (lower.contains(w)) hit = true;
        if (hit) {
            int c = highlightColor.getColor() & 0xFFFFFF;
            e.message = Text.literal("▶ ").styled(s -> s.withColor(c).withBold(true)).append(e.message);
            highlightSound.play();
        }
    }
}

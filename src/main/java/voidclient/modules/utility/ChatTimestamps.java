package voidclient.modules.utility;

import net.minecraft.text.Text;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.ModeSetting;
import voidclient.events.Events;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Prefixes chat messages with the time they arrived. */
public class ChatTimestamps extends Module {
    public final ModeSetting format = add(new ModeSetting("Format", "Time format.", "HH:mm", "HH:mm", "HH:mm:ss", "h:mm a", "h:mm:ss a"));
    public final ModeSetting brackets = add(new ModeSetting("Brackets", "Bracket style.", "[ ]", "[ ]", "( )", "< >", "None"));
    public final ColorSetting color = add(new ColorSetting("Color", "Timestamp colour.", 0xFF8E8AA8, false));

    public ChatTimestamps() {
        super("Chat Timestamps", "Adds the arrival time to every chat message.", Category.UTILITY);
        on(Events.ChatReceive.class, 50, e -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern(format.get()));
            String s = switch (brackets.get()) {
                case "( )" -> "(" + time + ") ";
                case "< >" -> "<" + time + "> ";
                case "None" -> time + " ";
                default -> "[" + time + "] ";
            };
            int c = color.getColor() & 0xFFFFFF;
            e.message = Text.literal(s).styled(st -> st.withColor(c)).append(e.message);
        });
    }
}

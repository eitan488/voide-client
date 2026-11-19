package voidclient.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatUtil {
    private ChatUtil() {}

    public static MutableText prefix() {
        return Text.literal("[").formatted(Formatting.DARK_GRAY)
                .append(Text.literal("VOID").styled(s -> s.withColor(0xB026FF).withBold(true)))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
    }

    public static void info(String msg) { send(Text.literal(msg).formatted(Formatting.GRAY)); }

    public static void send(Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud != null) mc.inGameHud.getChatHud().addMessage(prefix().append(text));
    }
}

package voidclient.utilities;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import voidclient.VoidClient;
import voidclient.gui.screens.ClickGuiScreen;
import voidclient.modules.world.WaypointStore;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** /void client commands: gui, toggle, profile, friend, waypoint. */
public final class ClientCommands {
    private ClientCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(literal("void")
                .then(literal("gui").executes(c -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    mc.send(() -> mc.setScreen(new ClickGuiScreen(null)));
                    return 1;
                }))
                .then(literal("toggle").then(argument("module", StringArgumentType.greedyString()).executes(c -> {
                    String name = StringArgumentType.getString(c, "module");
                    var m = VoidClient.get().modules().byName(name);
                    if (m.isEmpty()) {
                        c.getSource().sendError(Text.literal("Unknown module: " + name));
                        return 0;
                    }
                    VoidClient.get().modules().userToggle(m.get(), true);
                    ChatUtil.info(m.get().getName() + (m.get().isEnabled() ? " enabled" : " disabled"));
                    return 1;
                })))
                .then(literal("profile").then(argument("name", StringArgumentType.greedyString()).executes(c -> {
                    String name = StringArgumentType.getString(c, "name");
                    if (!VoidClient.get().profiles().exists(name)) {
                        c.getSource().sendError(Text.literal("No profile named " + name));
                        return 0;
                    }
                    VoidClient.get().profiles().switchTo(name);
                    return 1;
                })))
                .then(literal("friend")
                        .then(literal("add").then(argument("name", StringArgumentType.word()).executes(c -> {
                            FriendStore.add(StringArgumentType.getString(c, "name"));
                            ChatUtil.info("Friend added");
                            return 1;
                        })))
                        .then(literal("remove").then(argument("name", StringArgumentType.word()).executes(c -> {
                            FriendStore.remove(StringArgumentType.getString(c, "name"));
                            ChatUtil.info("Friend removed");
                            return 1;
                        })))
                        .then(literal("list").executes(c -> {
                            ChatUtil.info("Friends: " + String.join(", ", FriendStore.all()));
                            return 1;
                        })))
                .then(literal("waypoint").then(literal("add").then(argument("name", StringArgumentType.greedyString()).executes(c -> {
                    var p = MinecraftClient.getInstance().player;
                    if (p == null) return 0;
                    var w = WaypointStore.add(StringArgumentType.getString(c, "name"), p.getBlockX(), p.getBlockY(), p.getBlockZ(), 0xFF00F0FF, "Manual");
                    ChatUtil.info(w == null ? "Join a world first" : "Waypoint added");
                    return 1;
                }))).then(literal("goto").then(argument("x", IntegerArgumentType.integer()).then(argument("z", IntegerArgumentType.integer()).executes(c -> {
                    var p = MinecraftClient.getInstance().player;
                    if (p == null) return 0;
                    WaypointStore.add("Target", IntegerArgumentType.getInteger(c, "x"), p.getBlockY(), IntegerArgumentType.getInteger(c, "z"), 0xFFFF2BD6, "Manual");
                    ChatUtil.info("Target waypoint added");
                    return 1;
                })))))));
    }
}

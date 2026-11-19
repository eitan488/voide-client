package voidclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import voidclient.VoidClient;
import voidclient.events.Events;

/** Observes incoming play packets. Events are delivered on the client thread (read-only). */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static void voidclient$onReceive(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (VoidClient.get() == null || listener.getSide() != NetworkSide.CLIENTBOUND) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isOnThread()) VoidClient.get().events().post(new Events.PacketReceive(packet));
        else mc.execute(() -> VoidClient.get().events().post(new Events.PacketReceive(packet)));
    }
}

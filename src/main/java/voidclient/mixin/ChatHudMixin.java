package voidclient.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import voidclient.VoidClient;
import voidclient.events.Events;
import voidclient.modules.utility.ChatCustomizer;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @WrapMethod(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V")
    private void voidclient$addMessage(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, Operation<Void> original) {
        if (VoidClient.get() == null) {
            original.call(message, signature, indicator);
            return;
        }
        Events.ChatReceive e = VoidClient.get().events().post(new Events.ChatReceive(message));
        if (!e.isCancelled()) original.call(e.message, signature, indicator);
    }

    @ModifyConstant(method = {"addVisibleMessage", "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"}, constant = @Constant(intValue = 100))
    private int voidclient$historySize(int original) {
        return ChatCustomizer.historySize(original);
    }
}

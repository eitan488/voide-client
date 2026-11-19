package voidclient.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import voidclient.gui.screens.ScreenReplacer;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin implements ScreenReplacer.MultiplayerScreenAccess {
    @Shadow @Final private Screen parent;

    @Override
    public Screen voidclient$parent() { return parent; }
}

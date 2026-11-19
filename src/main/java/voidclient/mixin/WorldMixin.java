package voidclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import voidclient.modules.visuals.TimeChanger;
import voidclient.modules.visuals.WeatherChanger;

@Mixin(World.class)
public abstract class WorldMixin {
    @ModifyReturnValue(method = "getTimeOfDay", at = @At("RETURN"))
    private long voidclient$time(long original) {
        return (Object) this instanceof ClientWorld ? TimeChanger.modify(original) : original;
    }

    @ModifyReturnValue(method = "getRainGradient", at = @At("RETURN"))
    private float voidclient$rain(float original) {
        return (Object) this instanceof ClientWorld ? WeatherChanger.rain(original) : original;
    }

    @ModifyReturnValue(method = "getThunderGradient", at = @At("RETURN"))
    private float voidclient$thunder(float original) {
        return (Object) this instanceof ClientWorld ? WeatherChanger.thunder(original) : original;
    }
}

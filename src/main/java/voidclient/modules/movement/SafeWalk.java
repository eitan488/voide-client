package voidclient.modules.movement;

import net.minecraft.item.BlockItem;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;

/** Stops you walking off ledges, exactly like sneaking does (vanilla ledge clipping). */
public class SafeWalk extends Module {
    public static SafeWalk INSTANCE;

    public final BooleanSetting onlyBlocks = add(new BooleanSetting("Only Holding Blocks", "Active only while holding a block (bridging).", false));
    public final BooleanSetting onlyGround = add(new BooleanSetting("Only On Ground", "Ignore while airborne.", true));

    public SafeWalk() {
        super("Safe Walk", "Prevents walking off block edges.", Category.MOVEMENT);
        INSTANCE = this;
    }

    public static boolean shouldClip() {
        SafeWalk s = INSTANCE;
        if (s == null || !s.isEnabled() || mc.player == null) return false;
        if (s.onlyGround.get() && !mc.player.isOnGround()) return false;
        return !s.onlyBlocks.get() || mc.player.getMainHandStack().getItem() instanceof BlockItem || mc.player.getOffHandStack().getItem() instanceof BlockItem;
    }
}

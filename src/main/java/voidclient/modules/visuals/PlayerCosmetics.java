package voidclient.modules.visuals;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import voidclient.core.Category;
import voidclient.core.Module;
import voidclient.core.setting.BooleanSetting;
import voidclient.core.setting.ColorSetting;
import voidclient.core.setting.MultiSelectSetting;
import voidclient.core.setting.NumberSetting;
import voidclient.events.Events;
import voidclient.render.RenderManager;
import voidclient.utilities.ColorUtil;
import voidclient.utilities.FriendStore;

import java.util.List;
import java.util.Set;

/** Client-side cosmetics drawn on your model (visible only to you): halo, crown, orbiting orbs. */
public class PlayerCosmetics extends Module {
    public final MultiSelectSetting cosmetics = add(new MultiSelectSetting("Cosmetics", "Which cosmetics to show.", List.of("Halo", "Crown", "Orbs"), Set.of("Halo")));
    public final ColorSetting color = add(new ColorSetting("Color", "Cosmetic colour.", 0xFFFFD86B).themed());
    public final NumberSetting size = add(new NumberSetting("Size", "Cosmetic size.", 1, 0.5, 2, 0.05));
    public final NumberSetting heightOffset = add(new NumberSetting("Height Offset", "Extra height above the head.", 0.15, -0.3, 1, 0.05));
    public final NumberSetting spin = add(new NumberSetting("Spin Speed", "Rotation speed.", 1, 0, 5, 0.1));
    public final BooleanSetting friends = add(new BooleanSetting("Show On Friends", "Also decorate friends.", false));

    public PlayerCosmetics() {
        super("Player Model Cosmetics", "Halo, crown and orbs on your player model (client-side).", Category.VISUALS);
        on(Events.WorldRender.class, e -> {
            if (mc.world == null || mc.player == null) return;
            for (PlayerEntity p : mc.world.getPlayers()) {
                boolean self = p == mc.player;
                if (!self && !(friends.get() && FriendStore.isFriend(p.getGameProfile().name()))) continue;
                if (self && mc.options.getPerspective().isFirstPerson()) continue;
                draw(p.getLerpedPos(e.tickDelta).add(0, p.getHeight() + heightOffset.get(), 0));
            }
        });
    }

    private void draw(Vec3d top) {
        double t = System.currentTimeMillis() / 1000.0 * spin.get();
        double s = size.get();
        int c = color.getColor();
        if (cosmetics.isSelected("Halo")) {
            Vec3d h = top.add(0, 0.1 * s + Math.sin(t * 2) * 0.03, 0);
            RenderManager.ring(h, 0.3 * s, c, 3f, 40, false);
            RenderManager.ring(h, 0.33 * s, ColorUtil.multiplyAlpha(c, 0.35f), 6f, 40, false);
        }
        if (cosmetics.isSelected("Crown")) {
            Vec3d base = top.add(0, -0.08, 0);
            int spikes = 6;
            for (int i = 0; i < spikes; i++) {
                double a1 = t + Math.PI * 2 * i / spikes, a2 = t + Math.PI * 2 * (i + 0.5) / spikes, a3 = t + Math.PI * 2 * (i + 1) / spikes;
                double r = 0.26 * s;
                Vec3d p1 = base.add(Math.cos(a1) * r, 0, Math.sin(a1) * r);
                Vec3d tip = base.add(Math.cos(a2) * r, 0.2 * s, Math.sin(a2) * r);
                Vec3d p3 = base.add(Math.cos(a3) * r, 0, Math.sin(a3) * r);
                RenderManager.line(p1, tip, c, 2.5f, false);
                RenderManager.line(tip, p3, c, 2.5f, false);
                RenderManager.line(p1, p3, c, 2.5f, false);
            }
        }
        if (cosmetics.isSelected("Orbs")) {
            for (int i = 0; i < 3; i++) {
                double a = t * 2 + i * Math.PI * 2 / 3;
                Vec3d o = top.add(Math.cos(a) * 0.6 * s, -0.5 + Math.sin(t * 3 + i) * 0.2, Math.sin(a) * 0.6 * s);
                RenderManager.billboard(o, (float) (0.1 * s), c, false);
                RenderManager.billboard(o, (float) (0.25 * s), ColorUtil.multiplyAlpha(c, 0.3f), false);
            }
        }
    }
}

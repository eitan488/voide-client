package voidclient.modules.basehunting;

import net.minecraft.entity.Entity;
import voidclient.core.setting.ModeSetting;
import voidclient.events.Events;
import voidclient.render.EntityOutlines;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Entity ESP: box and/or vanilla glow outline for entities picked by {@link #accepts}. */
public abstract class EntityEspBase extends EspModule implements EntityOutlines.Provider {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Box, glow outline or both.", "Box", "Box", "Glow", "Both"));

    protected EntityEspBase(String name, String description) {
        super(name, description);
        EntityOutlines.register(this);
        on(Events.WorldRender.class, e -> {
            if (mc.world == null || mc.player == null || mode.is("Glow")) return;
            List<Entity> list = new ArrayList<>();
            for (Entity ent : mc.world.getEntities()) if (ent != mc.player && accepts(ent) && inRange(ent.getEntityPos())) list.add(ent);
            list.sort(Comparator.comparingDouble(x -> x.squaredDistanceTo(mc.player)));
            int n = 0;
            for (Entity ent : list) {
                if (n++ >= maxCount.getInt()) break;
                var lerped = ent.getLerpedPos(e.tickDelta);
                var box = ent.getBoundingBox().offset(lerped.subtract(ent.getEntityPos()));
                int c = colorFor(ent);
                drawBox(box, c);
                drawLabel(label(ent), lerped.add(0, ent.getHeight() + 0.3, 0), c);
            }
        });
    }

    protected abstract boolean accepts(Entity e);

    protected int colorFor(Entity e) { return color.getColor(); }

    protected String label(Entity e) { return e.getName().getString(); }

    @Override
    public boolean outlineActive() { return isEnabled() && !mode.is("Box"); }

    @Override
    public int outlineColor(Entity e) {
        if (e == mc.player || !accepts(e) || !inRange(e.getEntityPos())) return 0;
        return (colorFor(e) & 0xFFFFFF) | 0x01000000;
    }
}

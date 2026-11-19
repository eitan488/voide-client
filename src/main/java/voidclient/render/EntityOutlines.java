package voidclient.render;

import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes the vanilla entity-outline (glow) post effect to Void modules. Providers return an
 * RGB colour for entities they want outlined, or 0.
 */
public final class EntityOutlines {
    public interface Provider {
        boolean outlineActive();

        int outlineColor(Entity entity);
    }

    private static final List<Provider> PROVIDERS = new ArrayList<>();

    private EntityOutlines() {}

    public static void register(Provider p) { PROVIDERS.add(p); }

    public static int color(Entity entity) {
        for (Provider p : PROVIDERS) {
            if (!p.outlineActive()) continue;
            int c = p.outlineColor(entity);
            if (c != 0) return c;
        }
        return 0;
    }

    public static boolean shouldOutline(Entity entity) {
        if (PROVIDERS.isEmpty()) return false;
        return color(entity) != 0;
    }
}

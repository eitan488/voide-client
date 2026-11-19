package voidclient.events;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** All client events in one place. Mutable singletons are reused for hot-path events. */
public final class Events {
    private Events() {}

    public static final class TickPre { public static final TickPre INSTANCE = new TickPre(); }
    public static final class TickPost { public static final TickPost INSTANCE = new TickPost(); }

    /** Fired inside the HUD layer; modules draw 2D overlays here. */
    public static final class HudRender {
        public DrawContext ctx;
        public float tickDelta;
        public int width, height;
        /** True while F1 hides the HUD; overlays that belong to the HUD should skip drawing. */
        public boolean hudHidden;
    }

    /** Fired at END_MAIN of the world pass with camera-relative matrices. */
    public static final class WorldRender {
        public WorldRenderContext context;
        public MatrixStack matrices;
        public VertexConsumerProvider consumers;
        public Vec3d cameraPos;
        public float tickDelta;
    }

    public static final class Key extends Cancellable {
        public final int key, scancode, action, modifiers;
        public Key(int key, int scancode, int action, int modifiers) {
            this.key = key; this.scancode = scancode; this.action = action; this.modifiers = modifiers;
        }
    }

    public static final class MouseButton extends Cancellable {
        public final int button, action;
        public MouseButton(int button, int action) { this.button = button; this.action = action; }
    }

    public static final class MouseScroll extends Cancellable {
        public final double vertical;
        public MouseScroll(double vertical) { this.vertical = vertical; }
    }

    public static final class ChatReceive extends Cancellable {
        public Text message;
        public ChatReceive(Text message) { this.message = message; }
    }

    public static final class PacketReceive extends Cancellable {
        public final Packet<?> packet;
        public PacketReceive(Packet<?> packet) { this.packet = packet; }
    }

    public static final class PacketSend extends Cancellable {
        public final Packet<?> packet;
        public PacketSend(Packet<?> packet) { this.packet = packet; }
    }

    public record AttackEntity(Entity target) {}
    public record AttackBlock(BlockPos pos, Direction direction) {}
    public record BlockPlaced(BlockPos pos) {}
    public record HealthChange(LivingEntity entity, float oldHealth, float newHealth) {}
    public record TotemPop(Entity entity, int pops) {}
    public record Explosion(Vec3d pos, float radius) {}
    public record WorldJoin(String worldKey) {}
    public record WorldLeave() {}
    public record EntityRemoved(Entity entity) {}
    public record Death() {}
}

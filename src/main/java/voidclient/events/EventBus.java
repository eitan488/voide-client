package voidclient.events;

import voidclient.VoidClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Typed, priority-ordered event bus. Listener arrays are rebuilt on (un)subscribe so dispatch
 * is a plain array walk with no allocation. A throwing listener is reported to its owner's
 * error handler instead of crashing the game.
 */
public final class EventBus {
    public static final class Listener<E> {
        final Class<E> type;
        final Consumer<E> handler;
        final int priority;
        final ErrorHandler owner;
        boolean active;

        Listener(Class<E> type, Consumer<E> handler, int priority, ErrorHandler owner) {
            this.type = type;
            this.handler = handler;
            this.priority = priority;
            this.owner = owner;
        }

        public boolean isActive() { return active; }
    }

    public interface ErrorHandler {
        void onListenerError(Throwable t);
    }

    private static final Listener<?>[] EMPTY = new Listener<?>[0];
    private final Map<Class<?>, Listener<?>[]> listeners = new IdentityHashMap<>();

    public <E> Listener<E> create(Class<E> type, Consumer<E> handler, int priority, ErrorHandler owner) {
        return new Listener<>(type, handler, priority, owner);
    }

    public synchronized void register(Listener<?> l) {
        if (l.active) return;
        l.active = true;
        Listener<?>[] cur = listeners.getOrDefault(l.type, EMPTY);
        List<Listener<?>> next = new ArrayList<>(Arrays.asList(cur));
        next.add(l);
        next.sort(Comparator.comparingInt((Listener<?> x) -> -x.priority));
        listeners.put(l.type, next.toArray(EMPTY));
    }

    public synchronized void unregister(Listener<?> l) {
        if (!l.active) return;
        l.active = false;
        Listener<?>[] cur = listeners.getOrDefault(l.type, EMPTY);
        List<Listener<?>> next = new ArrayList<>(Arrays.asList(cur));
        next.remove(l);
        listeners.put(l.type, next.toArray(EMPTY));
    }

    /** Convenience for permanent system listeners. */
    public <E> Listener<E> subscribe(Class<E> type, Consumer<E> handler) {
        Listener<E> l = create(type, handler, 0, t -> VoidClient.LOG.error("System listener failed for {}", type.getSimpleName(), t));
        register(l);
        return l;
    }

    @SuppressWarnings("unchecked")
    public <E> E post(E event) {
        Listener<?>[] arr;
        synchronized (this) {
            arr = listeners.get(event.getClass());
        }
        if (arr == null) return event;
        for (Listener<?> raw : arr) {
            Listener<E> l = (Listener<E>) raw;
            if (!l.active) continue;
            try {
                l.handler.accept(event);
            } catch (Throwable t) {
                l.owner.onListenerError(t);
            }
            if (event instanceof Cancellable c && c.isCancelled()) break;
        }
        return event;
    }

    public boolean hasListeners(Class<?> type) {
        Listener<?>[] arr = listeners.get(type);
        return arr != null && arr.length > 0;
    }
}

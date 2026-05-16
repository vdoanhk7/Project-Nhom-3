package com.nhom3.client.event;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientEventBus {
    private static final Logger logger = LoggerFactory.getLogger(ClientEventBus.class);
    private static final ClientEventBus DEFAULT = new ClientEventBus();

    private final Map<Class<?>, CopyOnWriteArrayList<Subscriber<?, ?>>> subscribers =
            new ConcurrentHashMap<>();

    private ClientEventBus() {
    }

    public static ClientEventBus getDefault() {
        return DEFAULT;
    }

    public <O, E> Subscription subscribe(Class<E> eventType, O owner, BiConsumer<O, E> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(handler, "handler");

        Subscriber<O, E> subscriber = new Subscriber<>(owner, handler);
        subscribers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(subscriber);
        return () -> unsubscribe(eventType, subscriber);
    }

    public void unsubscribeOwner(Object owner) {
        if (owner == null) {
            return;
        }

        for (CopyOnWriteArrayList<Subscriber<?, ?>> eventSubscribers : subscribers.values()) {
            eventSubscribers.removeIf(subscriber -> subscriber.isDead() || subscriber.belongsTo(owner));
        }
    }

    public <E> void publish(E event) {
        if (event == null) {
            return;
        }

        CopyOnWriteArrayList<Subscriber<?, ?>> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers == null) {
            return;
        }

        for (Subscriber<?, ?> subscriber : eventSubscribers) {
            try {
                if (!subscriber.dispatch(event)) {
                    eventSubscribers.remove(subscriber);
                }
            } catch (RuntimeException e) {
                logger.error("Lỗi khi phát event {}", event.getClass().getSimpleName(), e);
            }
        }
    }

    private void unsubscribe(Class<?> eventType, Subscriber<?, ?> subscriber) {
        CopyOnWriteArrayList<Subscriber<?, ?>> eventSubscribers = subscribers.get(eventType);
        if (eventSubscribers != null) {
            eventSubscribers.remove(subscriber);
        }
    }

    @FunctionalInterface
    public interface Subscription {
        void unsubscribe();
    }

    private static final class Subscriber<O, E> {
        private final WeakReference<O> ownerRef;
        private final BiConsumer<O, E> handler;

        private Subscriber(O owner, BiConsumer<O, E> handler) {
            this.ownerRef = new WeakReference<>(owner);
            this.handler = handler;
        }

        private boolean dispatch(Object event) {
            O owner = ownerRef.get();
            if (owner == null) {
                return false;
            }

            handler.accept(owner, castEvent(event));
            return true;
        }

        private boolean isDead() {
            return ownerRef.get() == null;
        }

        private boolean belongsTo(Object owner) {
            return ownerRef.get() == owner;
        }

        @SuppressWarnings("unchecked")
        private E castEvent(Object event) {
            return (E) event;
        }
    }
}

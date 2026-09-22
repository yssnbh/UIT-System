package com.uit.shared.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-process events. Publish from the JavaFX thread when a listener updates the screen.
 */
public final class EventBus {

    private final Map<Class<? extends AppEvent>, List<Consumer<? extends AppEvent>>> listeners = new ConcurrentHashMap<>();

    public <T extends AppEvent> void subscribe(Class<T> type, Consumer<T> listener) {
        listeners.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void publish(AppEvent event) {
        List<Consumer<? extends AppEvent>> found = listeners.get(event.getClass());
        if (found == null) {
            return;
        }
        for (Consumer<? extends AppEvent> listener : found) {
            @SuppressWarnings("unchecked")
            Consumer<AppEvent> typed = (Consumer<AppEvent>) listener;
            typed.accept(event);
        }
    }
}

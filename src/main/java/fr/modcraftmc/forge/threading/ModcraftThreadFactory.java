package fr.modcraftmc.forge.threading;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModcraftThreadFactory {

    private static HashMap<String, ExecutorService> executors = new HashMap();
    public static final ExecutorService CHAT_THREAD = Executors.newCachedThreadPool(new NamedThreadFactory("Async Chat Thread"));
    public static final ExecutorService MODCRAFTMC_ASYNC = Executors.newSingleThreadExecutor(new NamedThreadFactory("Modcraft Async Thread"));

    public ModcraftThreadFactory() {
    }

    public static ExecutorService registerExecutor(ThreadSettings settings) {
        if (executors.containsKey(settings.name)) {
            throw new IllegalStateException("Cannot add two executor with the same name");
        } else {
            switch(settings.type) {
                case NORMAL:
                    executors.put(settings.name, Executors.newFixedThreadPool(settings.threads, new NamedThreadFactory(settings.name)));
                case CACHED:
                    executors.put(settings.name, Executors.newCachedThreadPool(new NamedThreadFactory(settings.name)));
                default:
                    return get(settings.name);
            }
        }
    }

    public static void unregisterExecutor(String name) {
        if (!executors.containsKey(name)) {
            throw new IllegalStateException("No executor with this name exist.");
        } else {
            executors.remove(name);
        }
    }

    public static ExecutorService get(String name) {
        if (!executors.containsKey(name)) {
            throw new IllegalStateException("No executor with this name exist.");
        } else {
            return (ExecutorService)executors.get(name);
        }
    }
}

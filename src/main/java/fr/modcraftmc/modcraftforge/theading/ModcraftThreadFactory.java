package fr.modcraftmc.modcraftforge.theading;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModcraftThreadFactory {

    private static HashMap<String ,ExecutorService> executors = new HashMap<>();

    public static final ExecutorService LOGIN_THREAD = Executors.newFixedThreadPool(2, new NamedThreadFactory("Player login thread"));

    public static final ExecutorService CHAT_THREAD = Executors.newSingleThreadExecutor(new NamedThreadFactory("Chat thread"));




    public static class AssignableThread extends Thread {
        public AssignableThread(Runnable run) {
            super(run);
        }
    }

    public static void registerExecutor(String name, int threads) {

        if (executors.containsKey(name)) throw new IllegalArgumentException("Cannot add two executor with the same name");

        executors.put(name, Executors.newFixedThreadPool(threads, new NamedThreadFactory(name)));

    }

    public static void unregisterExecutor(String name) {

        if (!executors.containsKey(name)) throw new IllegalArgumentException("No executor with this name exist.");

        executors.remove(name);
    }

    public static ExecutorService get(String name) {

        if (!executors.containsKey(name)) throw new IllegalArgumentException("No executor with this name exist.");

        return executors.get(name);

    }

}

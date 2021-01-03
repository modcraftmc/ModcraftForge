package fr.modcraftmc.forge.threading;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {
    private String name;

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    public Thread newThread(@NotNull Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(this.name);
        return thread;
    }
}

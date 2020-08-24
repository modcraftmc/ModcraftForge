package net.minecraftforge.modcraftforge.common.theading;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {
    private String name;

    public NamedThreadFactory(String name) {
        this.name = name;
    }


    @Override
    public Thread newThread(@NotNull Runnable r) {
        Thread thread = new ModcraftThreadBox.AssignableThread(r);
        thread.setName(name);
        return thread;
    }
}

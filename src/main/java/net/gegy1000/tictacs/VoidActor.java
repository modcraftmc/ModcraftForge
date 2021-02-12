package net.gegy1000.tictacs;

import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import net.minecraft.util.concurrent.ITaskQueue;

public final class VoidActor extends DelegatedTaskExecutor<Runnable> {
    public VoidActor(String name) {
        super(new VoidQueue(), runnable -> {}, name);
    }

    @Override
    public void run() {
    }

    @Override
    public void enqueue(Runnable message) {
    }

    @Override
    public void close() {
    }

    private static class VoidQueue implements ITaskQueue<Runnable, Runnable> {

        @Override
        public Runnable poll() {
            return null;
        }

        @Override
        public boolean enqueue(Runnable message) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    }
}

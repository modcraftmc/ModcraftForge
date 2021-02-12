package net.gegy1000.tictacs.lighting;


import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorldLightManager;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

// TODO: consolidate lighting workers?
public final class LightingExecutor implements Runnable, AutoCloseable {
    private final WorldLightManager lightingProvider;

    private volatile Queues queues = new Queues();
    private volatile Queues processingQueues = new Queues();

    private final Thread thread;
    private volatile boolean closed;

    private final AtomicBoolean parked = new AtomicBoolean();

    public LightingExecutor(WorldLightManager lightingProvider) {
        this.lightingProvider = lightingProvider;

        this.thread = new Thread(this);
        this.thread.setName("lighting-worker");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void enqueue(Runnable task, ServerWorldLightManager.Phase stage) {
        this.queues.enqueue(task, stage);
        this.wake();
    }

    public void wake() {
        if (this.hasTasks() && this.parked.compareAndSet(true, false)) {
            LockSupport.unpark(this.thread);
        }
    }

    @Override
    public void run() {
        while (!this.closed) {
            if (this.hasTasks()) {
                Queues queues = this.queues;
                this.queues = this.processingQueues;
                this.processingQueues = queues;

                this.runTasks(queues);
            } else {
                this.parked.set(true);
                LockSupport.park();
            }
        }
    }

    private boolean hasTasks() {
        return !this.queues.isEmpty() || this.lightingProvider.hasLightWork();
    }

    private void runTasks(Queues queues) {
        this.processQueue(queues.preUpdate);
        this.lightingProvider.tick(Integer.MAX_VALUE, true, true);
        this.processQueue(queues.postUpdate);
    }

    private void processQueue(Queue<Runnable> queue) {
        Runnable task;
        while ((task = queue.poll()) != null) {
            task.run();
        }
    }

    @Override
    public void close() {
        this.closed = true;
        this.wake();
    }

    private static class Queues {
        final Queue<Runnable> preUpdate = new ConcurrentLinkedQueue<>();
        final Queue<Runnable> postUpdate = new ConcurrentLinkedQueue<>();

        void enqueue(Runnable task, ServerWorldLightManager.Phase stage) {
            if (stage == ServerWorldLightManager.Phase.PRE_UPDATE) {
                this.preUpdate.add(task);
            } else {
                this.postUpdate.add(task);
            }
        }

        boolean isEmpty() {
            return this.preUpdate.isEmpty() && this.postUpdate.isEmpty();
        }
    }
}

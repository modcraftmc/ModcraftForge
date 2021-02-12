package net.gegy1000.justnow.executor;

import net.gegy1000.justnow.future.Future;

import java.util.concurrent.ThreadFactory;

public final class ThreadedExecutor implements AutoCloseable {
    private final Worker[] workers;
    private final TaskQueue taskQueue = new TaskQueue();

    private boolean active = true;

    public ThreadedExecutor(int threadCount, ThreadFactory factory) {
        this.workers = new Worker[threadCount];
        for (int i = 0; i < threadCount; i++) {
            this.workers[i] = new Worker(factory);
        }
        for (Worker worker : this.workers) {
            worker.start();
        }
    }

    public <T> TaskHandle<T> spawn(Future<T> future) {
        Task<T> task = new Task<>(future, this.taskQueue);
        this.taskQueue.enqueue(task);
        return task.handle;
    }

    public <T> Future<T> steal(TaskHandle<T> handle) {
        this.taskQueue.remove(handle.task);
        return handle.steal();
    }

    public boolean cancel(TaskHandle<?> handle) {
        if (this.taskQueue.remove(handle.task)) {
            handle.invalidate();
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        this.active = false;
        this.taskQueue.clear();
        for (Worker worker : this.workers) {
            worker.thread.interrupt();
        }
    }

    private class Worker {
        private final Thread thread;

        Worker(ThreadFactory factory) {
            this.thread = factory.newThread(this::drive);
        }

        public void start() {
            this.thread.start();
        }

        private void drive() {
            try {
                while (ThreadedExecutor.this.active) {
                    Task<?> task = ThreadedExecutor.this.taskQueue.take();
                    task.advance();
                }
            } catch (InterruptedException e) {
                // interrupted by executor
            }
        }
    }
}

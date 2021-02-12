package net.gegy1000.justnow.executor;

import net.gegy1000.justnow.future.Future;

import java.util.concurrent.atomic.AtomicBoolean;

final class Task<T> {
    final Future<T> future;
    final TaskQueue.Waker waker;

    TaskHandle<T> handle;

    private final AtomicBoolean invalidated = new AtomicBoolean(false);

    Task(Future<T> future, TaskQueue taskQueue) {
        this.future = future;
        this.handle = new TaskHandle<>(this);
        this.waker = taskQueue.waker(this);
    }

    void invalidate() {
        this.invalidated.set(true);
    }

    boolean isInvalid() {
        return this.invalidated.get();
    }

    void advance() {
        if (this.isInvalid()) return;

        try {
            this.waker.polling();

            T result = this.future.poll(this.waker);
            if (result != null) {
                this.invalidate();
                this.handle.completeOk(result);
            } else {
                this.waker.ready();
            }
        } catch (Throwable exception) {
            this.invalidate();
            this.handle.completeErr(exception);
        }
    }
}

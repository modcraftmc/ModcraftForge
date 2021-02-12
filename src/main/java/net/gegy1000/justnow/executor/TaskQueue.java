package net.gegy1000.justnow.executor;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public final class TaskQueue {
    private final LinkedBlockingDeque<Task<?>> tasks = new LinkedBlockingDeque<>();

    public void clear() {
        this.tasks.clear();
    }

    public void enqueue(Task<?> task) {
        if (task.isInvalid()) return;
        this.tasks.add(task);
    }

    public boolean remove(Task<?> task) {
        return this.tasks.remove(task);
    }

    public Task<?> take() throws InterruptedException {
        return this.tasks.take();
    }

    public void drainTo(Collection<Task<?>> target) {
        while (!this.tasks.isEmpty()) {
            target.add(this.tasks.remove());
        }
    }

    public boolean isEmpty() {
        return this.tasks.isEmpty();
    }

    public Waker waker(Task task) {
        return new Waker(task);
    }

    public class Waker implements net.gegy1000.justnow.Waker {
        static final int READY = 0;
        static final int POLLING = 1;
        static final int AWOKEN = 2;

        private final Task<?> task;

        final AtomicInteger state = new AtomicInteger(AWOKEN);

        private Waker(Task<?> task) {
            this.task = task;
        }

        @Override
        public void wake() {
            // if we are currently polling, set state to awoken and don't re-enqueue the task until we are ready again
            if (this.state.compareAndSet(POLLING, AWOKEN)) {
                return;
            }

            // if we are currently ready, set state to awoken and re-enqueue the task
            if (this.state.compareAndSet(READY, AWOKEN)) {
                TaskQueue.this.enqueue(this.task);
            }
        }

        void polling() {
            this.state.set(POLLING);
        }

        void ready() {
            // we didn't get a result: set state to ready. we expect state to still be polling, so if that's not
            // the case, we must've been awoken during polling. now that we know this task needs to continue
            // execution, we can re-enqueue it.
            if (!this.state.compareAndSet(Waker.POLLING, Waker.READY)) {
                TaskQueue.this.enqueue(this.task);
            }
        }
    }
}

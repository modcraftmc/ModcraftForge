package net.gegy1000.justnow.executor;

import net.gegy1000.justnow.future.Future;
import net.gegy1000.justnow.future.JoinHandle;

public final class TaskHandle<T> extends JoinHandle<T> {
    final Task<T> task;

    TaskHandle(Task<T> task) {
        this.task = task;
    }

    @Override
    protected synchronized void completeOk(T result) {
        super.completeOk(result);
    }

    @Override
    protected synchronized void completeErr(Throwable exception) {
        super.completeErr(exception);
    }

    void invalidate() {
        this.task.invalidate();
    }

    Future<T> steal() {
        this.task.invalidate();
        if (this.result != null) {
            return Future.ready(this.result);
        }
        return this.task.future;
    }
}

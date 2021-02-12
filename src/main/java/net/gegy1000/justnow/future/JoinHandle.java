package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;

import javax.annotation.Nullable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

public class JoinHandle<T> implements Future<T> {
    protected T result;
    protected Throwable exception;

    protected Thread executingThread;
    protected Waker waker;

    @Nullable
    @Override
    public synchronized T poll(Waker waker) {
        this.waker = waker;
        if (this.exception != null) {
            throw encodeException(this.exception);
        }
        return this.result;
    }

    public synchronized void cancel() {
        this.cancel(new CancellationException("task canceled"));
    }

    public synchronized void cancel(Throwable exception) {
        this.exception = exception;
        if (this.waker != null) {
            this.waker.wake();
        }
        if (this.executingThread != null) {
            this.executingThread.interrupt();
        }
    }

    public synchronized boolean isDone() {
        return this.result != null || this.exception != null;
    }

    protected synchronized void setExecutingThread(Thread executingThread) {
        this.executingThread = executingThread;
    }

    protected synchronized void completeOk(T result) {
        if (result == null) throw new IllegalArgumentException("cannot complete with null result");
        if (this.isDone()) throw new IllegalStateException("already completed");

        this.result = result;
        if (this.waker != null) {
            this.waker.wake();
        }
    }

    protected synchronized void completeErr(Throwable exception) {
        if (exception == null) throw new IllegalArgumentException("cannot complete with null exception");
        if (this.isDone()) throw new IllegalStateException("already completed");

        this.exception = exception;
        if (this.waker != null) {
            this.waker.wake();
        }
    }

    private static CompletionException encodeException(Throwable exception) {
        if (exception instanceof CompletionException) {
            return (CompletionException) exception;
        } else {
            return new CompletionException(exception);
        }
    }
}

package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;
import net.gegy1000.justnow.tuple.Unit;

import javax.annotation.Nullable;

public final class MaybeDone<T> implements Future<Unit> {
    private final Future<T> future;
    private T result;

    MaybeDone(Future<T> future) {
        this.future = future;
    }

    @Override
    public Unit poll(Waker waker) {
        if (this.result == null) {
            this.result = this.future.poll(waker);
        }
        return this.isDone() ? Unit.INSTANCE : null;
    }

    @Nullable
    public T getResult() {
        return this.result;
    }

    public boolean isDone() {
        return this.result != null;
    }
}

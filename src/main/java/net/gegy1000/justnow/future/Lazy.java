package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;

import java.util.function.Supplier;

final class Lazy<T> implements Future<T> {
    private final Supplier<T> op;

    Lazy(Supplier<T> op) {
        this.op = op;
    }

    @Override
    public T poll(Waker waker) {
        return this.op.get();
    }
}

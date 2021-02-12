package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;

final class Ready<T> implements Future<T> {
    private final T value;

    Ready(T value) {
        this.value = value;
    }

    @Override
    public T poll(Waker waker) {
        return this.value;
    }
}

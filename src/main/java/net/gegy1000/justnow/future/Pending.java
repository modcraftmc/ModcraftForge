package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;

final class Pending<T> implements Future<T> {
    @Override
    public T poll(Waker waker) {
        return null;
    }
}

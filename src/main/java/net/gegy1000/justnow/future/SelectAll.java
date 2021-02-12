package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;

import java.util.Collection;

final class SelectAll<T> implements Future<T> {
    private final Collection<Future<T>> futures;

    SelectAll(Collection<Future<T>> futures) {
        this.futures = futures;
    }

    @Override
    public T poll(Waker waker) {
        for (Future<T> future : this.futures) {
            T poll = future.poll(waker);
            if (poll != null) {
                return poll;
            }
        }
        return null;
    }
}

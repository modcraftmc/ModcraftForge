package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;

import java.util.function.BiFunction;

final class Map2<A, B, R> implements Future<R> {
    private final MaybeDone<A> a;
    private final MaybeDone<B> b;
    private final BiFunction<A, B, R> map;

    Map2(Future<A> a, Future<B> b, BiFunction<A, B, R> map) {
        this.a = Future.maybeDone(a);
        this.b = Future.maybeDone(b);
        this.map = map;
    }

    @Override
    public R poll(Waker waker) {
        this.a.poll(waker);
        this.b.poll(waker);

        if (this.a.isDone() && this.b.isDone()) {
            return this.map.apply(this.a.getResult(), this.b.getResult());
        }

        return null;
    }
}

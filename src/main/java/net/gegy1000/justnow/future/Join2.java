package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;
import net.gegy1000.justnow.tuple.Two;

final class Join2<A, B> implements Future<Two<A, B>> {
    private final MaybeDone<A> a;
    private final MaybeDone<B> b;

    Join2(Future<A> a, Future<B> b) {
        this.a = Future.maybeDone(a);
        this.b = Future.maybeDone(b);
    }

    @Override
    public Two<A, B> poll(Waker waker) {
        this.a.poll(waker);
        this.b.poll(waker);

        if (this.a.isDone() && this.b.isDone()) {
            return Two.of(this.a.getResult(), this.b.getResult());
        }

        return null;
    }
}

package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;
import net.gegy1000.justnow.tuple.Either;

final class Select2<A, B> implements Future<Either<A, B>> {
    private final MaybeDone<A> a;
    private final MaybeDone<B> b;

    Select2(Future<A> a, Future<B> b) {
        this.a = Future.maybeDone(a);
        this.b = Future.maybeDone(b);
    }

    @Override
    public Either<A, B> poll(Waker waker) {
        this.a.poll(waker);
        this.b.poll(waker);

        if (this.a.isDone()) {
            return Either.a(this.a.getResult());
        } else if (this.b.isDone()) {
            return Either.b(this.b.getResult());
        }

        return null;
    }
}

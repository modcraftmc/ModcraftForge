package net.gegy1000.justnow.tuple;

import javax.annotation.Nullable;

public final class Either<A, B> {
    private final A a;
    private final B b;

    private Either(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public static <A, B> Either<A, B> a(A a) {
        if (a == null) throw new IllegalArgumentException("A cannot be null");
        return new Either<>(a, null);
    }

    public static <A, B> Either<A, B> b(B b) {
        if (b == null) throw new IllegalArgumentException("B cannot be null");
        return new Either<>(null, b);
    }

    public boolean isA() {
        return this.a != null;
    }

    public boolean isB() {
        return this.b != null;
    }

    @Nullable
    public A getA() {
        return this.a;
    }

    @Nullable
    public B getB() {
        return this.b;
    }
}

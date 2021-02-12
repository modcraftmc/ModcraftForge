package net.gegy1000.justnow.tuple;

public final class Two<A, B> {
    public final A a;
    public final B b;

    private Two(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public static <A, B> Two<A, B> of(A a, B b) {
        return new Two<>(a, b);
    }
}

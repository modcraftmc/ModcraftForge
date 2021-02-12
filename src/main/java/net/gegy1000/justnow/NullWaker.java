package net.gegy1000.justnow;

public final class NullWaker implements Waker {
    public static final NullWaker INSTANCE = new NullWaker();

    private NullWaker() {
    }

    @Override
    public void wake() {
    }
}

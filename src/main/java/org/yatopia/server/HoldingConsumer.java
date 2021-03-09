package org.yatopia.server;

import java.util.function.Consumer;

public class HoldingConsumer<T> implements Consumer<T> {

    private T value;

    @Override
    public void accept(T t) {
        this.value = t;
    }

    public T getValue() {
        return value;
    }
}

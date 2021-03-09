package me.jellysquid.mods.lithium.common.util;


import org.yatopia.server.HoldingConsumer;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Producer<T> {
    /**
     * Computes the next sequence of values in a collection. If a null value is passed for {@param consumer}, then
     * the producer will only return whether or not elements existed.
     *
     * @param consumer The (nullable) consumer which will accept the computed values during this run.
     * @return True if the producer produced any values, otherwise false
     */
    boolean computeNext(Consumer<? super T> consumer);

    default <U> Producer<U> map(Function<T, U> mapper) {
        return consumer -> {
            Consumer<? super T> con = (t) -> consumer.accept(mapper.apply(t));
            return Producer.this.computeNext(con);
        };
    }

    static <T> Stream<T> asStream(Producer<T> producer) {
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                return producer.computeNext(action);
            }
        }, false);
    }

    static <T> void fillList(Producer<T> producer, List<T> list) {
        HoldingConsumer<T> consumer = new HoldingConsumer<>();
        while (producer.computeNext(consumer)) {
            T value = consumer.getValue();
            if (value == null || list.contains(value)) { continue; }
            list.add(value);
        }
    }

    Producer<?> EMPTY_PRODUCER = consumer -> false;

    @SuppressWarnings("unchecked")
    static <T> Producer<T> empty() {
        return (Producer<T>) EMPTY_PRODUCER;
    }
}

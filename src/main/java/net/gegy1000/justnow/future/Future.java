package net.gegy1000.justnow.future;

import net.gegy1000.justnow.Waker;
import net.gegy1000.justnow.tuple.Either;
import net.gegy1000.justnow.tuple.Two;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Future is the most basic unit of asynchronous execution: it represents a value that will resolve at some point in
 * the future.
 *
 * In this library, futures are driven by polling. This means that to get the output of a future, it needs to be asked
 * whether it is ready or not. This means that the polling handler should never block: it should always return
 * effectively immediately. For code that must block, use {@link Future#spawnBlocking(Executor, Supplier)}
 *
 * The biggest benefit of a polling-based design is the ability to drive multiple futures concurrently on a single
 * thread. It is additionally possible to drive futures from a thread without consuming that thread. This removes any
 * overhead from having lots of threads by allowing all the work to happen on a single thread.
 *
 * For long running tasks, it is not ideal to poll continually and cause the thread to spin and waste resources.
 * This is solved by the {@link Waker} interface: when a future is polled, if it does not return a result, it should
 * register the waker internally. When the future's result is ready, {@link Waker#wake()} should be called. If it is not
 * called, the future will not be polled again.
 * @see JoinHandle for a reference implementation of how to use wakers
 *
 * While it is possible to write a polling handler that does block, caution should be taken to avoid this as it can
 * create unexpected inefficiency in execution by preventing futures from being run concurrently.
 * @see Future#poll(Waker) for more information on polling
 *
 * When a future is resolved is not guaranteed: a future may never become ready. For example, {@link Future#pending()}
 * will never resolve into a value.
 *
 * To drive a future, you generally do not want to be interacting with {@link Future#poll(Waker)} directly. Rather, you
 * should use an executor. In this library, 3 executors are provided out of the box:
 * - {@link net.gegy1000.justnow.executor.CurrentThreadExecutor} provides utilities for driving and blocking on a future
 * - {@link net.gegy1000.justnow.executor.LocalExecutor} allows running many futures concurrently on the local thread
 * - {@link net.gegy1000.justnow.executor.ThreadedExecutor} allows running many futures concurrently on a thread pool
 *
 * @param <T> the type that this future will resolve to when ready
 */
public interface Future<T> {
    /**
     * Polls a future for its result.
     * If the future is still pending a result, null should be returned
     * If the future is ready, it should return its result
     *
     * Once this function has returned a non-null ready value, it should <b>not</b> be called again. Doing so may result
     * in undefined behavior.
     * To handle situations where you must do this, wrap the future with {@link Future#maybeDone(Future)}
     *
     * @param waker the waker to notify when this future becomes ready
     * @return null if pending or T if ready
     */
    @Nullable
    T poll(Waker waker);

    /**
     * Spawns a blocking task onto the given executor. The task is <b>not</b> allowed to return a null value, if it must
     * return an optional value, use {@link java.util.Optional}.
     *
     * Returns a {@link JoinHandle} future which can be used to track the result of the task.
     * <b>NB</b>: the handle is not attached to the execution of the task: it does not need to be polled in order for
     * the task to be executed.
     *
     * A blocking task is one that should take a long time to execute or cannot return effectively instantly
     *   @see Future for further discussion around futures design
     *
     * @param executor the executor to execute the blocking task on
     * @param supplier the blocking task
     * @param <T> the future output type
     *
     * @return the handle for this task's execution
     */
    static <T> JoinHandle<T> spawnBlocking(Executor executor, Supplier<T> supplier) {
        JoinHandle<T> handle = new JoinHandle<>();
        executor.execute(() -> {
            handle.setExecutingThread(Thread.currentThread());
            T result = supplier.get();
            handle.completeOk(result);
        });
        return handle;
    }

    /**
     * Returns a future that will always be in a ready state with the given return value
     *
     * @param value the value to be ready with (cannot be null)
     * @param <T> the future output type
     * @return an always ready future
     */
    static <T> Future<T> ready(T value) {
        if (value == null) throw new IllegalArgumentException("ready value cannot be null");
        return new Ready<>(value);
    }

    /**
     * Returns a future that will always be in the pending state. Polling will never return a ready result.
     *
     * @param <T> the future output type
     * @return an always pending future
     */
    static <T> Future<T> pending() {
        return new Pending<>();
    }

    /**
     * Returns a future that lazily evaluates its result. Polling should return a ready result immediately, but the
     * supplier will only be invoked when the future is polled.
     *
     * @param op the operator to lazily evaluate
     * @param <T> the future output type
     * @return a lazily evaluated future
     */
    static <T> Future<T> lazy(Supplier<T> op) {
        return new Lazy<>(op);
    }

    /**
     * Wraps a future allowing its execution to be canceled. When this future is canceled,
     * it will yield a {@link java.util.concurrent.CancellationException} which must be handled
     *
     * @param future the future to wrap
     * @param <T> the future output type
     * @return a cancelable future
     */
    static <T> Cancelable<T> cancelable(Future<T> future) {
        return new Cancelable<>(future);
    }

    /**
     * Wraps a future allowing its result to be retrieved even after it has completed. A MaybeDone future should still
     * be polled, however once it has completed, the result is accessible through {@link MaybeDone#getResult()}
     *
     * @param future the future to wrap
     * @param <T> the future output type
     * @return a MaybeDone future
     */
    static <T> MaybeDone<T> maybeDone(Future<T> future) {
        return new MaybeDone<>(future);
    }

    static <A, B> Future<Two<A, B>> join2(Future<A> a, Future<B> b) {
        return new Join2<>(a, b);
    }

    static <A, B, R> Future<Either<A, B>> select2(Future<A> a, Future<B> b) {
        return new Select2<>(a, b);
    }

    static <T> Future<Collection<T>> joinAll(Collection<Future<T>> futures) {
        return new JoinAll<>(futures);
    }

    static <T> Future<Collection<T>> joinAll(Stream<Future<T>> futures) {
        return new JoinAll<>(futures.collect(Collectors.toCollection(LinkedList::new)));
    }

    static <K, V> Future<Map<K, V>> joinAll(Map<K, Future<V>> map) {
        return new JoinAllMap<>(map);
    }

    static <T> Future<T> selectAll(Collection<Future<T>> futures) {
        return new SelectAll<>(futures);
    }

    static <A, B, R> Future<R> map2(Future<A> a, Future<B> b, BiFunction<A, B, R> map) {
        return new Map2<>(a, b, map);
    }

    static <A, B, R> Future<R> andThen2(Future<A> a, Future<B> b, BiFunction<A, B, Future<R>> andThen) {
        return new Map2<>(a, b, andThen).andThen(f -> f);
    }

    default <U> Future<U> map(Function<T, U> map) {
        return new Map1<>(this, map);
    }

    default <U> Future<U> andThen(Function<T, Future<U>> andThen) {
        return new AndThen<>(this, andThen);
    }

    default <U> Future<U> handle(BiFunction<T, Throwable, U> handle) {
        return new Handle<>(this, handle);
    }

    default <U> Future<Two<T, U>> join(Future<U> future) {
        return new Join2<>(this, future);
    }

    default <U> Future<Either<T, U>> select(Future<U> future) {
        return new Select2<>(this, future);
    }
}

package net.gegy1000.justnow.executor;

import net.gegy1000.justnow.NullWaker;
import net.gegy1000.justnow.SignalWaker;
import net.gegy1000.justnow.future.Future;

import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;

public final class CurrentThreadExecutor {
    private static final ThreadLocal<SignalWaker> THREAD_WAKER = ThreadLocal.withInitial(SignalWaker::new);

    public static <T> T blockOn(Future<T> future) {
        try {
            SignalWaker waker = THREAD_WAKER.get();

            T result;
            while ((result = future.poll(waker)) == null) {
                waker.awaitSignal();
            }

            return result;
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    @Nullable
    public static <T> T advance(Future<T> future) {
        return future.poll(NullWaker.INSTANCE);
    }
}

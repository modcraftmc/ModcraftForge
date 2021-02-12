package net.gegy1000.tictacs.chunk.future;

import com.mojang.datafixers.util.Either;
import net.gegy1000.tictacs.AtomicPool;
import net.gegy1000.tictacs.chunk.ChunkNotLoadedException;
import net.gegy1000.justnow.Waker;
import net.gegy1000.justnow.future.Future;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class VanillaChunkFuture implements Future<IChunk> {
    private static final AtomicPool<VanillaChunkFuture> POOL = new AtomicPool<>(512, VanillaChunkFuture::new);

    private volatile CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> inner;
    private volatile boolean listening;

    private VanillaChunkFuture() {
    }

    public static VanillaChunkFuture of(CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> completable) {
        VanillaChunkFuture future = POOL.acquire();
        future.init(completable);
        return future;
    }

    void init(CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> future) {
        this.inner = future;
        this.listening = false;
    }

    @Override
    public IChunk poll(Waker waker) {
        Either<IChunk, ChunkHolder.IChunkLoadingError> result = this.inner.getNow(null);
        if (result != null) {
            Optional<ChunkHolder.IChunkLoadingError> err = result.right();
            if (err.isPresent()) {
                throw ChunkNotLoadedException.INSTANCE;
            }

            IChunk chunk = result.left().get();
            this.release();

            return chunk;
        } else if (!this.listening) {
            this.listening = true;
            this.inner.handle((r, t) -> {
                waker.wake();
                return null;
            });
        }

        return null;
    }

    private void release() {
        this.inner = null;
        POOL.release(this);
    }
}

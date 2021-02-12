package net.gegy1000.tictacs.chunk.entry;

import com.mojang.datafixers.util.Either;
import net.gegy1000.tictacs.chunk.ChunkNotLoadedException;
import net.gegy1000.tictacs.chunk.future.SharedListener;
import net.gegy1000.tictacs.chunk.step.ChunkStep;

import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import java.util.concurrent.CompletableFuture;

public final class ChunkListener extends SharedListener<IChunk> {
    final ChunkEntry entry;
    final ChunkStep step;

    volatile boolean err;

    final CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> vanilla;

    ChunkListener(ChunkEntry entry, ChunkStep step) {
        this.entry = entry;
        this.step = step;

        IChunk chunk = this.getChunkForStep();
        if (chunk != null) {
            this.vanilla = CompletableFuture.completedFuture(Either.left(chunk));
        } else {
            this.vanilla = new CompletableFuture<>();
        }
    }

    @Override
    protected IChunk get() {
        if (this.err) {
            throw ChunkNotLoadedException.INSTANCE;
        }

        return this.getChunkForStep();
    }

    public void completeOk() {
        this.err = false;

        IChunk chunk = this.getChunkForStep();
        if (chunk == null) {
            throw new IllegalStateException("cannot complete chunk with null chunk");
        }

        this.vanilla.complete(Either.left(chunk));

        this.wake();
    }

    public void completeErr() {
        this.err = true;
        this.vanilla.complete(ChunkHolder.MISSING_CHUNK);

        this.wake();
    }

    private IChunk getChunkForStep() {
        return this.entry.getChunkForStep(this.step);
    }

    public CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> asVanilla() {
        return this.vanilla;
    }
}

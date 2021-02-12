package net.gegy1000.tictacs;

import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.minecraft.world.chunk.IChunk;

import java.util.concurrent.CompletableFuture;

public interface AsyncChunkAccess {
    IChunk getExistingChunk(int x, int z, ChunkStep step);

    IChunk getAnyExistingChunk(int x, int z);

    CompletableFuture<IChunk> getOrCreateChunkAsync(int x, int z, ChunkStep step);

    boolean shouldChunkExist(int x, int z, ChunkStep step);

    default boolean shouldChunkExist(int x, int z) {
        return this.shouldChunkExist(x, z, ChunkStep.FULL);
    }
}

package net.gegy1000.tictacs;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;

import java.util.concurrent.CompletableFuture;


public interface AsyncChunkIo {
    CompletableFuture<CompoundNBT> getNbtAsync(ChunkPos pos);
}

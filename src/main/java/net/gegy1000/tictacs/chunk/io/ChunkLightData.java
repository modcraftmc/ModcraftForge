package net.gegy1000.tictacs.chunk.io;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ServerWorld;

public interface ChunkLightData {
    void acceptSection(int sectionY, CompoundNBT sectionTag, ChunkStatus status);

    void applyToWorld(ChunkPos chunkPos, ServerWorld world);

    void applyToChunk(ChunkPrimer chunk);
}

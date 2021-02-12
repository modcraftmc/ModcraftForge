package net.gegy1000.tictacs.compatibility;


import net.minecraft.world.chunk.IChunk;

import java.util.concurrent.CompletableFuture;

public interface PhosphorServerLightingProviderAccess {
    CompletableFuture<IChunk> setupLightmaps(IChunk chunk);
}

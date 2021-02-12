package net.gegy1000.tictacs.compatibility;

import net.gegy1000.justnow.future.Future;
import net.gegy1000.tictacs.chunk.future.FutureHandle;
import net.gegy1000.tictacs.chunk.step.ChunkStepContext;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.fml.ModList;

import java.util.concurrent.CompletableFuture;

public final class TicTacsCompatibility {
    public static final boolean STARLIGHT_LOADED = ModList.get().isLoaded("starlight");
    public static final boolean PHOSPHOR_LOADED = ModList.get().isLoaded("phosphor");

    public static Future<IChunk> afterFeaturesStep(ChunkStepContext ctx) {
        if (PHOSPHOR_LOADED) {
            return afterFeaturesStepPhosphor(ctx);
        } else {
            return Future.ready(ctx.chunk);
        }
    }

    private static Future<IChunk> afterFeaturesStepPhosphor(ChunkStepContext ctx) {
        FutureHandle<IChunk> handle = new FutureHandle<>();

        CompletableFuture<IChunk> future = ((PhosphorServerLightingProviderAccess) ctx.lighting).setupLightmaps(ctx.chunk);
        future.thenAccept(handle::complete);

        return handle;
    }
}

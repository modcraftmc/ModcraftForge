package net.gegy1000.tictacs.chunk;

import net.gegy1000.justnow.future.Future;
import net.gegy1000.justnow.tuple.Unit;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.gegy1000.tictacs.chunk.entry.ChunkListener;
import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.gegy1000.tictacs.chunk.tracker.ChunkTracker;
import net.gegy1000.tictacs.chunk.upgrade.ChunkUpgrader;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.TicketManager;

public interface ChunkController {
    default ChunkManager asTacs() {
        return (ChunkManager) this;
    }

    ChunkMap getMap();

    ChunkUpgrader getUpgrader();

    TicketManager getTicketManager();

    ChunkTracker getTracker();

    ChunkListener getChunkAs(ChunkEntry entry, ChunkStep step);

    Future<Unit> getRadiusAs(ChunkPos pos, int radius, ChunkStep step);

    Future<IChunk> spawnLoadChunk(ChunkEntry entry);

    void notifyStatus(ChunkPos pos, ChunkStatus status);

    <T> void spawnOnMainThread(ChunkEntry entry, Future<T> future);

    default <T> void spawnOnMainThread(Future<T> future) {
        this.spawnOnMainThread(null, future);
    }

    void spawnOnMainThread(ChunkEntry entry, Runnable runnable);
}

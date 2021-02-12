package net.gegy1000.tictacs.chunk.upgrade;

import net.gegy1000.justnow.future.Future;
import net.gegy1000.justnow.tuple.Unit;
import net.gegy1000.tictacs.async.lock.Lock;
import net.gegy1000.tictacs.async.lock.NullLock;
import net.gegy1000.tictacs.async.lock.Semaphore;
import net.gegy1000.tictacs.async.worker.ChunkExecutor;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.gegy1000.tictacs.chunk.step.ChunkStepContext;
import net.gegy1000.tictacs.compatibility.TicTacsCompatibility;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;

import java.util.List;

public final class ChunkUpgrader {
    private final ChunkExecutor worker = ChunkExecutor.INSTANCE;

    private final ChunkController controller;

    private final ServerWorld world;
    private final ChunkGenerator generator;
    private final TemplateManager structures;
    private final ServerWorldLightManager lighting;

    public final Lock lightingThrottler = TicTacsCompatibility.STARLIGHT_LOADED ? NullLock.INSTANCE : new Semaphore(32);

    public ChunkUpgrader(
            ServerWorld world,
            ChunkController controller,
            ChunkGenerator generator,
            TemplateManager structures,
            ServerWorldLightManager lighting
    ) {
        this.world = world;
        this.generator = generator;
        this.structures = structures;
        this.lighting = lighting;

        this.controller = controller;
    }

    public void spawnUpgradeTo(ChunkEntry entry, ChunkStep step) {
        if (entry.trySpawnUpgradeTo(step)) {
            this.worker.spawn(entry, this.upgradeTo(entry, step));
        }
    }

    public Future<IChunk> loadChunk(ChunkEntry entry) {
        if (entry.trySpawnLoad()) {
            return new ChunkLoadFuture(this.controller, entry);
        } else {
            return entry.getListenerFor(ChunkStep.EMPTY);
        }
    }

    private Future<Unit> upgradeTo(ChunkEntry entry, ChunkStep step) {
        // TODO: pool instances
        return new ChunkUpgradeFuture(this.controller, entry, step);
    }

    Future<IChunk> runStepTask(ChunkEntry entry, ChunkStep step, List<IChunk> chunks) {
        // TODO: reuse context objects
        ChunkStepContext context = new ChunkStepContext(this.controller, entry, this.world, this.generator, this.structures, this.lighting, entry.getProtoChunk(), chunks);

        if (this.hasAlreadyUpgradedTo(entry, step)) {
            return step.runLoad(context);
        } else {
            return step.runUpgrade(context);
        }
    }

    private boolean hasAlreadyUpgradedTo(ChunkEntry entry, ChunkStep step) {
        ChunkPrimer currentChunk = entry.getProtoChunk();
        return currentChunk != null && currentChunk.getStatus().isAtLeast(step.getMaximumStatus());
    }

    void notifyUpgradeOk(ChunkEntry entry, ChunkStep step, IChunk chunk) {
        entry.completeUpgradeOk(step, chunk);

        ChunkStatus status = step.getMaximumStatus();

        this.controller.notifyStatus(entry.getPosition(), status);
        ChunkStep.trySetStatus(chunk, status);
    }

    void notifyUpgradeUnloaded(ChunkEntry entry, ChunkStep step) {
        entry.notifyUpgradeUnloaded(step);
        this.controller.notifyStatus(entry.getPosition(), null);
    }
}

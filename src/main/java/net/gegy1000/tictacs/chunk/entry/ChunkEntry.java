package net.gegy1000.tictacs.chunk.entry;

import com.mojang.datafixers.util.Either;
import net.gegy1000.tictacs.QueuingConnection;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.ChunkLevelTracker;
import net.gegy1000.tictacs.chunk.ChunkMap;
import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

public final class ChunkEntry extends ChunkHolder {
    private final AtomicReferenceArray<ChunkListener> listeners = new AtomicReferenceArray<>(ChunkStep.STEPS.size());

    private volatile ChunkPrimer chunk;
    private volatile Chunk worldChunk;

    private volatile ChunkStep currentStep;
    private final AtomicReference<ChunkStep> spawnedStep = new AtomicReference<>();
    private final AtomicBoolean loading = new AtomicBoolean();

    private final ChunkEntryTrackers trackers = new ChunkEntryTrackers();
    private final ChunkAccessLock lock = new ChunkAccessLock();

    public ChunkEntry(
            ChunkPos pos, int level,
            WorldLightManager lighting,
            ChunkHolder.IListener levelUpdateListener,
            ChunkHolder.IPlayerProvider watchers
    ) {
        super(pos, level, lighting, levelUpdateListener, watchers);
    }

    public ChunkAccessLock getLock() {
        return this.lock;
    }

    public ChunkListener getListenerFor(ChunkStep step) {
        while (true) {
            ChunkListener listener = this.listeners.get(step.getIndex());
            if (listener != null) {
                return listener;
            }

            ChunkListener newListener = new ChunkListener(this, step);
            if (this.listeners.compareAndSet(step.getIndex(), null, newListener)) {
                for (ChunkStatus status : step.getStatuses()) {
                    this.field_219312_g.set(status.ordinal(), newListener.asVanilla());
                }

                return newListener;
            }
        }
    }

    public ChunkListener getValidListenerFor(ChunkStep step) {
        return this.isValidAs(step) ? this.getListenerFor(step) : null;
    }

    public ChunkStep getCurrentStep() {
        return this.currentStep;
    }

    public boolean canUpgradeTo(ChunkStep toStep) {
        return this.isValidAs(toStep) && !this.isAt(toStep);
    }

    public boolean isValidAs(ChunkStep toStep) {
        int requiredLevel = ChunkLevelTracker.FULL_LEVEL + ChunkStep.getDistanceFromFull(toStep);
        return this.chunkLevel <= requiredLevel;
    }

    public ChunkStep getTargetStep() {
        return getTargetStep(this.chunkLevel);
    }

    public static ChunkStep getTargetStep(int level) {
        int distanceFromFull = level - ChunkLevelTracker.FULL_LEVEL;
        return ChunkStep.byDistanceFromFull(distanceFromFull);
    }

    public boolean trySpawnUpgradeTo(ChunkStep toStep) {
        if (!this.isValidAs(toStep)) {
            return false;
        }

        while (true) {
            ChunkStep fromStep = this.spawnedStep.get();
            if (fromStep != null && fromStep.greaterOrEqual(toStep)) {
                return false;
            }

            if (this.spawnedStep.compareAndSet(fromStep, toStep)) {
                this.combineSavingFuture(toStep);
                return true;
            }
        }
    }

    public boolean trySpawnLoad() {
        return this.loading.compareAndSet(false, true);
    }

    public boolean isTicking() {
        Either<Chunk, ChunkHolder.IChunkLoadingError> ticking = this.getTickingFuture().getNow(null);
        if (ticking == null) {
            return false;
        }

        return !ticking.right().isPresent();
    }

    public boolean isTickingEntities() {
        Either<Chunk, ChunkHolder.IChunkLoadingError> entityTicking = this.getEntityTickingFuture().getNow(null);
        if (entityTicking == null) {
            return false;
        }

        return !entityTicking.right().isPresent();
    }

    public void onUpdateLevel(ChunkManager tacs) {
        if (this.chunkLevel > this.prevChunkLevel) {
            this.reduceLevel(this.prevChunkLevel, this.chunkLevel);

            ChunkHolder.LocationType level = getLocationTypeFromLevel(this.chunkLevel);
            ChunkHolder.LocationType lastLevel = getLocationTypeFromLevel(this.prevChunkLevel);

            // TODO: better unify logic that adds & removes from the trackable chunk list
            if (!level.isAtLeast(LocationType.TICKING) && lastLevel.isAtLeast(LocationType.TICKING)) {
                ChunkMap map = ((ChunkController) tacs).getMap();
                map.getTickingMaps().removeTrackableChunk(this);
            }
        }

        super.processUpdates(tacs);
    }

    private void reduceLevel(int lastLevel, int level) {
        boolean wasLoaded = ChunkLevelTracker.isLoaded(lastLevel);
        if (!wasLoaded) {
            return;
        }

        boolean isLoaded = ChunkLevelTracker.isLoaded(level);

        ChunkStep lastStep = getTargetStep(lastLevel);
        ChunkStep targetStep = getTargetStep(level);

        int startIdx = isLoaded ? targetStep.getIndex() + 1 : 0;
        int endIdx = lastStep.getIndex();

        if (startIdx > endIdx) {
            return;
        }

        for (int i = startIdx; i <= endIdx; i++) {
            ChunkListener listener = this.listeners.getAndSet(i, null);
            if (listener != null) {
                listener.completeErr();
            }
        }

        this.downgradeSpawnedStep(targetStep);
    }

    private void downgradeSpawnedStep(ChunkStep targetStep) {
        while (true) {
            ChunkStep spawnedStep = this.spawnedStep.get();
            if (targetStep != null && !targetStep.lessThan(spawnedStep)) {
                break;
            }

            if (this.spawnedStep.compareAndSet(spawnedStep, targetStep)) {
                break;
            }
        }
    }

    public ChunkPrimer getProtoChunk() {
        return this.chunk;
    }

    @Override
    public Chunk getChunkIfComplete() {
        return this.worldChunk;
    }

    public IChunk getChunk() {
        Chunk worldChunk = this.worldChunk;
        if (worldChunk != null) {
            return worldChunk;
        }
        return this.chunk;
    }

    public IChunk getChunkAtLeast(ChunkStep step) {
        if (this.isAt(step)) {
            return this.getChunk();
        } else {
            return null;
        }
    }

    public IChunk getChunkForStep(ChunkStep step) {
        if (!this.isAt(step)) {
            return null;
        }

        if (step == ChunkStep.FULL) {
            return this.worldChunk;
        } else {
            return this.chunk;
        }
    }

    public boolean isAt(ChunkStep step) {
        return step.lessOrEqual(this.currentStep);
    }

    public void completeUpgradeOk(ChunkStep step, IChunk chunk) {
        ChunkStep lastStep = this.includeStep(step);

        if (chunk instanceof ChunkPrimer) {
            this.chunk = (ChunkPrimer) chunk;
        }

        int startIdx = lastStep != null ? lastStep.getIndex() : 0;
        int endIdx = step.getIndex();

        for (int idx = startIdx; idx <= endIdx; idx++) {
            ChunkListener listener = this.listeners.get(idx);
            if (listener != null) {
                listener.completeOk();
            }
        }
    }

    public void notifyUpgradeUnloaded(ChunkStep step) {
        for (int i = step.getIndex(); i < this.listeners.length(); i++) {
            ChunkListener listener = this.listeners.getAndSet(i, null);
            if (listener != null) {
                listener.completeErr();
            }
        }

        this.notifyUpgradeCanceled(step);
    }

    public void notifyUpgradeCanceled(ChunkStep step) {
        this.downgradeSpawnedStep(step.getPrevious());
    }

    ChunkStep includeStep(ChunkStep step) {
        ChunkStep currentStep = this.currentStep;
        if (step.greaterOrEqual(currentStep)) {
            this.currentStep = step;
        }
        return currentStep;
    }

    void combineSavingFuture(ChunkStep step) {
        this.chain(this.getListenerFor(step).asVanilla());
    }

    void combineSavingFuture(IChunk chunk) {
        this.chain(CompletableFuture.completedFuture(Either.left(chunk)));
    }


    public Chunk finalizeChunk(ServerWorld world, LongPredicate loadToWorld) {
        if (this.worldChunk != null) {
            throw new IllegalStateException("chunk already finalized!");
        }

        Chunk worldChunk = unwrapWorldChunk(this.chunk);
        if (worldChunk == null) {
            worldChunk = this.upgradeToWorldChunk(world, this.chunk);
        }

        this.worldChunk = worldChunk;
        this.combineSavingFuture(this.worldChunk);

        worldChunk.setLocationType(() -> ChunkHolder.getLocationTypeFromLevel(this.chunkLevel));
        worldChunk.postLoad();

        if (loadToWorld.test(this.pos.asLong())) {
            worldChunk.setLoaded(true);
            world.addTileEntities(worldChunk.getTileEntityMap().values());

            Collection<Entity> invalidEntities = this.tryAddEntitiesToWorld(world, worldChunk);
            invalidEntities.forEach(worldChunk::removeEntity);
        }

        worldChunk.rescheduleTicks();

        return worldChunk;
    }

    private Chunk upgradeToWorldChunk(ServerWorld world, ChunkPrimer protoChunk) {
        Chunk worldChunk = new Chunk(world, protoChunk);
        this.chunk = new ChunkPrimerWrapper(worldChunk);

        return worldChunk;
    }

    private Collection<Entity> tryAddEntitiesToWorld(ServerWorld world, Chunk chunk) {
        Collection<Entity> invalidEntities = new ArrayList<>();

        for (ClassInheritanceMultiMap<Entity> entitySection : chunk.getEntityLists()) {
            for (Entity entity : entitySection) {
                if (entity instanceof PlayerEntity) continue;

                if (!world.addEntity(entity)) {
                    invalidEntities.add(entity);
                }
            }
        }

        return invalidEntities;
    }

    private static Chunk unwrapWorldChunk(IChunk chunk) {
        if (chunk instanceof ChunkPrimerWrapper) {
            return ((ChunkPrimerWrapper) chunk).getChunk();
        }
        return null;
    }

    public ChunkEntryTrackers getTrackers() {
        return this.trackers;
    }

    @Override
    protected void sendToTracking(IPacket<?> packet, boolean onlyOnWatchDistanceEdge) {
        Set<ServerPlayerEntity> trackingPlayers = this.trackers.getTrackingPlayers();
        if (trackingPlayers.isEmpty()) {
            return;
        }

        if (!onlyOnWatchDistanceEdge) {
            for (ServerPlayerEntity player : trackingPlayers) {
                QueuingConnection.enqueueSend(player.connection, packet);
            }
        } else {
            // pass through TACS to filter the edge
            Stream<ServerPlayerEntity> players = this.playerProvider.getTrackingPlayers(this.pos, true);
            players.forEach(player -> QueuingConnection.enqueueSend(player.connection, packet));
        }
    }

    @Override
    @Deprecated
    public CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_219276_a(ChunkStatus status, ChunkManager tacs) {
        return tacs.func_219244_a(this, status);
    }

    @Override
    @Deprecated
    public CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_219301_a(ChunkStatus status) {
        ChunkStep step = ChunkStep.byStatus(status);
        return this.getListenerFor(step).asVanilla();
    }

    @Override
    @Deprecated
    public CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_225410_b(ChunkStatus status) {
        ChunkStep step = ChunkStep.byStatus(status);
        return this.isValidAs(step) ? this.func_219301_a(status) : ChunkHolder.MISSING_CHUNK_FUTURE;
    }

    @Override
    @Deprecated
    protected void processUpdates(ChunkManager tacs) {
        this.onUpdateLevel(tacs);
    }

    @Override
    @Deprecated
    public IChunk func_219287_e() {
        return this.getProtoChunk();
    }

    @Override
    @Deprecated
    public void func_219294_a(ChunkPrimerWrapper chunk) {
    }

    // TODO: Ideally we can avoid running this logic here, and instead have it be run when we're trying to start/stop chunk tracking
    public boolean isChunkTickable() {
        Set<ServerPlayerEntity> players = this.trackers.getTickableTrackingPlayers();
        if (players.isEmpty()) {
            return false;
        }

        for (ServerPlayerEntity player : players) {
            if (!player.isSpectator()) {
                return true;
            }
        }

        return false;
    }
}

package common.mixins.tictacs;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.gegy1000.justnow.future.Future;
import net.gegy1000.justnow.tuple.Unit;
import net.gegy1000.tictacs.VoidActor;
import net.gegy1000.tictacs.async.worker.ChunkMainThreadExecutor;
import net.gegy1000.tictacs.chunk.ChunkAccess;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.ChunkLevelTracker;
import net.gegy1000.tictacs.chunk.ChunkMap;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.gegy1000.tictacs.chunk.entry.ChunkListener;
import net.gegy1000.tictacs.chunk.future.AwaitAll;
import net.gegy1000.tictacs.chunk.future.ChunkNotLoadedFuture;
import net.gegy1000.tictacs.chunk.future.LazyRunnableFuture;
import net.gegy1000.tictacs.chunk.future.VanillaChunkFuture;
import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.gegy1000.tictacs.chunk.tracker.ChunkTracker;
import net.gegy1000.tictacs.chunk.upgrade.ChunkUpgrader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonPartEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.*;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Mixin(ChunkManager.class)
public abstract class ThreadedAnvilChunkStorageMixin implements ChunkController {
    @Shadow
    @Final
    private ThreadTaskExecutor<Runnable> mainThread;
    @Shadow
    @Final
    private ServerWorldLightManager lightManager;
    @Shadow
    @Final
    private ChunkManager.ProxyTicketManager ticketManager;
    @Shadow
    @Final
    private IChunkStatusListener field_219266_t;
    @Shadow
    @Final
    private AtomicInteger field_219268_v;
    @Shadow
    private int viewDistance;

    @Unique
    private ChunkMap map;
    @Unique
    private ChunkUpgrader upgrader;
    @Unique
    private ChunkTracker tracker;

    @Unique
    private ChunkLevelTracker levelTracker;

    @Unique
    private ChunkMainThreadExecutor chunkMainExecutor;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(
            ServerWorld world,
            SaveFormat.LevelSave levelSession,
            DataFixer dataFixer,
            TemplateManager structures,
            Executor threadPool,
            ThreadTaskExecutor<Runnable> mainThread,
            IChunkLightProvider chunkProvider,
            ChunkGenerator chunkGenerator,
            IChunkStatusListener progressListener,
            Supplier<DimensionSavedDataManager> persistentStateSupplier,
            int watchDistance,
            boolean syncWrite,
            CallbackInfo ci
    ) {
        ServerWorldLightManager lighting = this.lightManager;

        this.map = new ChunkMap(world, this);
        this.upgrader = new ChunkUpgrader(world, this, chunkGenerator, structures, lighting);

        this.levelTracker = new ChunkLevelTracker(world, this);

        this.tracker = new ChunkTracker(world, this);
        this.tracker.setViewDistance(this.viewDistance);

        this.map.addListener(this.tracker);

        this.chunkMainExecutor = new ChunkMainThreadExecutor(mainThread);
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 33))
    private static int getFullChunkLevel(int level) {
        return ChunkLevelTracker.FULL_LEVEL;
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/concurrent/DelegatedTaskExecutor;create(Ljava/util/concurrent/Executor;Ljava/lang/String;)Lnet/minecraft/util/concurrent/DelegatedTaskExecutor;",
                    ordinal = 0
            )
    )
    private DelegatedTaskExecutor<Runnable> createWorldgenActor(Executor executor, String name) {
        return new VoidActor(name);
    }

    @Override
    public ChunkMap getMap() {
        return this.map;
    }

    @Override
    public ChunkUpgrader getUpgrader() {
        return this.upgrader;
    }

    @Override
    public TicketManager getTicketManager() {
        return this.ticketManager;
    }

    @Override
    public ChunkTracker getTracker() {
        return this.tracker;
    }

    @Override
    public ChunkListener getChunkAs(ChunkEntry entry, ChunkStep step) {
        this.upgrader.spawnUpgradeTo(entry, step);
        return entry.getListenerFor(step);
    }

    @Override
    public Future<Unit> getRadiusAs(ChunkPos pos, int radius, ChunkStep step) {
        ChunkAccess chunks = this.map.visible();

        ChunkMap.FlushListener flushListener = this.map.awaitFlush();

        int size = radius * 2 + 1;
        Future<IChunk>[] futures = new Future[size * size];
        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                int idx = (x + radius) + (z + radius) * size;
                ChunkEntry entry = chunks.getEntry(pos.x + x, pos.z + z);
                if (entry == null) {
                    return flushListener.andThen(unit -> this.getRadiusAs(pos, radius, step));
                }

                if (entry.isValidAs(step)) {
                    this.upgrader.spawnUpgradeTo(entry, step);
                    futures[idx] = entry.getListenerFor(step);
                } else {
                    return ChunkNotLoadedFuture.get();
                }
            }
        }

        flushListener.invalidateWaker();

        return AwaitAll.of(futures);
    }

    @Override
    public Future<IChunk> spawnLoadChunk(ChunkEntry entry) {
        return VanillaChunkFuture.of(this.chunkLoad(entry.getPosition()));
    }

    @Override
    public void notifyStatus(ChunkPos pos, ChunkStatus status) {
        this.field_219266_t.statusChanged(pos, status);
    }

    @Override
    public <T> void spawnOnMainThread(ChunkEntry entry, Future<T> future) {
        this.chunkMainExecutor.spawn(entry, future);
    }

    @Override
    public void spawnOnMainThread(ChunkEntry entry, Runnable runnable) {
        this.chunkMainExecutor.spawn(entry, new LazyRunnableFuture(runnable));
    }

    /**
     * @reason delegate to ChunkMap
     * @author gegy1000
     */
    @Nullable
    @Overwrite
    public ChunkHolder func_219220_a(long pos) {
        return this.map.primary().getEntry(pos);
    }

    /**
     * @reason delegate to ChunkMap
     * @author gegy1000
     */
    @Nullable
    @Overwrite
    public ChunkHolder func_219219_b(long pos) {
        return this.map.visible().getEntry(pos);
    }

    /**
     * @reason replace usage of ChunkStatus and delegate to custom upgrader logic
     * @author gegy1000
     */
    @Overwrite
    public CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_219244_a(ChunkHolder holder, ChunkStatus status) {
        ChunkStep step = ChunkStep.byStatus(status);

        ChunkEntry entry = (ChunkEntry) holder;
        this.upgrader.spawnUpgradeTo(entry, step);

        return entry.getListenerFor(step).asVanilla();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(BooleanSupplier runWhile, CallbackInfo ci) {
        this.map.flushToVisible();
    }

    @Redirect(
            method = "scheduleUnloads",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;remove(J)Ljava/lang/Object;",
                    remap = false
            )
    )
    private Object removeChunkForUnload(Long2ObjectLinkedOpenHashMap<ChunkHolder> map, long pos) {
        return this.map.primary().removeEntry(pos);
    }

    @Redirect(
            method = "save(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;",
                    remap = false
            )
    )
    private ObjectCollection<?> getChunks(Long2ObjectLinkedOpenHashMap<?> map) {
        return this.map.primary().getEntries();
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void close(CallbackInfo ci) {
        this.chunkMainExecutor.close();
    }

    /**
     * @reason delegate to ChunkLevelTracker
     * @author gegy1000
     */
    @Nullable
    @Overwrite
    private ChunkHolder setChunkLevel(long pos, int toLevel, @Nullable ChunkHolder entry, int fromLevel) {
        return this.levelTracker.setLevel(pos, toLevel, (ChunkEntry) entry, fromLevel);
    }

    /**
     * @reason replace the level used for light tickets
     * @author gegy1000
     */
    @Overwrite
    public void func_219209_c(ChunkPos pos) {
        this.mainThread.enqueue(() -> {
            this.ticketManager.releaseWithLevel(TicketType.LIGHT, pos, ChunkLevelTracker.LIGHT_TICKET_LEVEL, pos);
        });
    }

    /**
     * @reason replace usage of async area-loading
     * @author gegy1000
     */
    @Overwrite
    public CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> func_219188_b(ChunkPos pos) {
        CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> future = new CompletableFuture<>();

        ChunkEntry entry = this.map.primary().getEntry(pos);

        this.spawnOnMainThread(entry, this.getRadiusAs(pos, 2, ChunkStep.FULL).handle((ok, err) -> {
            if (err == null && entry.getChunkIfComplete() != null) {
                future.complete(Either.left(entry.getChunkIfComplete()));
            } else {
                future.complete(ChunkHolder.UNLOADED_CHUNK);
            }
            return Unit.INSTANCE;
        }));

        return future;
    }

    /**
     * @reason replace usage of async area-loading
     * @author gegy1000
     */
    @Overwrite
    public CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> func_219179_a(ChunkHolder holder) {
        CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> future = new CompletableFuture<>();
        ChunkEntry entry = (ChunkEntry) holder;

        this.spawnOnMainThread(entry, this.getRadiusAs(entry.getPosition(), 1, ChunkStep.FULL)
                .handle((ok, err) -> {
                    if (err != null) {
                        future.complete(ChunkHolder.UNLOADED_CHUNK);
                        return Unit.INSTANCE;
                    }

                    Chunk chunk = entry.getChunkIfComplete();
                    if (chunk != null) {
                        chunk.postProcess();

                        this.field_219268_v.getAndIncrement();
                        this.tracker.onChunkFull(entry, chunk);
                        this.map.getTickingMaps().addTrackableChunk(entry);

                        future.complete(Either.left(chunk));
                    } else {
                        future.complete(ChunkHolder.UNLOADED_CHUNK);
                    }

                    return Unit.INSTANCE;
                })
        );

        return future;
    }

    /**
     * @reason delegate to ChunkMap
     * @author gegy1000
     */
    @Overwrite
    public boolean refreshOffThreadCache() {
        return this.map.flushToVisible();
    }

    /**
     * @reason delegate to ChunkMap
     * @author gegy1000
     */
    @Overwrite
    public int getLoadedChunkCount() {
        return this.map.getEntryCount();
    }

    /**
     * @reason delegate to ChunkMap
     * @author gegy1000
     */
    @Overwrite
    public Iterable<ChunkHolder> getLoadedChunksIterable() {
        return Iterables.unmodifiableIterable(this.map.visible().getEntries());
    }

    @ModifyConstant(method = "setViewDistance", constant = @Constant(intValue = 33))
    private int getMaxViewDistance(int level) {
        return ChunkLevelTracker.FULL_LEVEL;
    }

    @Redirect(
            method = "setViewDistance",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;",
                    remap = false
            )
    )
    private ObjectCollection<ChunkHolder> setViewDistance(Long2ObjectLinkedOpenHashMap<ChunkHolder> chunks) {
        if (this.tracker != null) {
            this.tracker.setViewDistance(this.viewDistance);
        }
        return ObjectLists.emptyList();
    }

    /**
     * @reason delegate to ChunkTracker
     * @author gegy1000
     */
    @Overwrite
    public void track(Entity entity) {
        if (entity instanceof EnderDragonPartEntity) {
            return;
        }
        this.tracker.getEntities().add(entity);
    }

    /**
     * @reason delegate to ChunkTracker
     * @author gegy1000
     */
    @Overwrite
    public void untrack(Entity entity) {
        this.tracker.getEntities().remove(entity);
    }

    /**
     * @reason delegate to ChunkTracker
     * @author gegy1000
     */
    @Overwrite
    public void sendToAllTracking(Entity entity, IPacket<?> packet) {
        this.tracker.getEntities().sendToTracking(entity, packet);
    }

    /**
     * @reason delegate to ChunkTracker
     * @author gegy1000
     */
    @Overwrite
    public void sendToTrackingAndSelf(Entity entity, IPacket<?> packet) {
        this.tracker.getEntities().sendToTrackingAndSelf(entity, packet);
    }

    /**
     * @reason use cached list of tracking players on the chunk entry
     * @author gegy1000
     */
    @Overwrite
    public boolean isOutsideSpawningRadius(ChunkPos chunkPos) {
        long pos = chunkPos.asLong();

        ChunkEntry entry = this.map.visible().getEntry(pos);
        return entry == null || !entry.isChunkTickable();
    }

    /**
     * @reason use cached list of tracking players on the chunk entry
     * @author gegy1000
     */
    @Redirect(
            method = "getTrackingPlayers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/PlayerGenerationTracker;getGeneratingPlayers(J)Ljava/util/stream/Stream;"
            )
    )
    private Stream<ServerPlayerEntity> getPlayersWatchingChunk(PlayerGenerationTracker watchManager, long pos) {
        return this.getPlayersWatchingChunk(pos);
    }

    private Stream<ServerPlayerEntity> getPlayersWatchingChunk(long pos) {
        ChunkEntry entry = this.map.visible().getEntry(pos);
        if (entry != null) {
            return entry.getTrackers().getTrackingPlayers().stream();
        }
        return Stream.empty();
    }

    /**
     * @reason delegate to ChunkTracker
     * @author gegy1000
     */
    @Overwrite
    public void tickEntityTracker() {
        this.tracker.tick();
    }

    /**
     * @reason we already detect player movement across chunks through normal entity tracker handling
     * @author gegy1000
     */
    @Inject(method = "updatePlayerPosition", at = @At("HEAD"), cancellable = true)
    private void updateCameraPosition(ServerPlayerEntity player, CallbackInfo ci) {
        ci.cancel();
    }

    @Shadow
    protected abstract CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> chunkLoad(ChunkPos pos);
}

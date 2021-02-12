package common.mixins.tictacs;

import com.mojang.datafixers.util.Either;
import net.gegy1000.tictacs.AsyncChunkAccess;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.ChunkLevelTracker;
import net.gegy1000.tictacs.chunk.LossyChunkCache;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(ServerChunkProvider.class)
public abstract class ServerChunkManagerMixin implements AsyncChunkAccess {
    @Shadow
    @Final
    private ServerWorld world;
    @Shadow
    @Final
    private TicketManager ticketManager;
    @Shadow
    @Final
    private ServerChunkProvider.ChunkExecutor executor;
    @Shadow
    @Final
    public ChunkManager chunkManager;

    @Shadow
    @Final
    private Thread mainThread;

    @Unique
    private final LossyChunkCache fastCache = new LossyChunkCache(32);

    @Inject(method = "invalidateCaches", at = @At("HEAD"))
    private void clearChunkCache(CallbackInfo ci) {
        this.fastCache.clear();
    }

    /**
     * @reason optimize chunk query and cache logic and avoid blocking the main thread if possible
     * @author gegy1000
     */
    @Overwrite
    @Nullable
    public IChunk getChunk(int x, int z, ChunkStatus status, boolean create) {
        ChunkStep step = ChunkStep.byStatus(status);
        if (create) {
            if (Thread.currentThread() != this.mainThread) {
                return this.getOrCreateChunkOffThread(x, z, step);
            } else {
                return this.getOrCreateChunkOnThread(x, z, step);
            }
        } else {
            return this.getExistingChunk(x, z, step);
        }
    }

    private IChunk getOrCreateChunkOnThread(int x, int z, ChunkStep step) {
        // first we test if the chunk already exists in our small cache
        IChunk cached = this.fastCache.get(x, z, step);
        if (cached != null) {
            return cached;
        }

        // if it does not exist, try load it from the chunk entry
        ChunkEntry entry = this.getChunkEntry(x, z);
        IChunk chunk = this.getExistingChunkFor(entry, step);

        // the chunk is not ready, we must interface and join the chunk future
        if (chunk == null) {
            Either<IChunk, ChunkHolder.IChunkLoadingError> result = this.joinFuture(this.createChunk(x, z, step));

            chunk = result.map(
                    Function.identity(),
                    err -> { throw new IllegalStateException("Chunk not there when requested: " + err); }
            );
        }

        this.fastCache.put(x, z, step, chunk);

        return chunk;
    }

    private <T> T joinFuture(CompletableFuture<T> future) {
        if (!future.isDone()) {
            this.executor.driveUntil(future::isDone);
        }
        return future.join();
    }

    private IChunk getOrCreateChunkOffThread(int x, int z, ChunkStep step) {
        Either<IChunk, ChunkHolder.IChunkLoadingError> result = CompletableFuture.supplyAsync(
                () -> this.createChunk(x, z, step),
                this.executor
        ).join().join();

        return result.map(
                chunk -> chunk,
                unloaded -> {
                    throw new IllegalStateException("Chunk not there when requested: " + unloaded);
                }
        );
    }

    /**
     * @reason optimize chunk query and cache logic and avoid blocking the main thread if possible
     * @author gegy1000
     */
    @Overwrite
    @Nullable
    public Chunk getChunkNow(int x, int z) {
        return (Chunk) this.getExistingChunk(x, z, ChunkStep.FULL);
    }

    /**
     * @reason optimize chunk query
     * @author gegy1000
     */
    @Overwrite
    @Nullable
    public IBlockReader getChunkForLight(int x, int z) {
        ChunkEntry entry = this.getChunkEntry(x, z);
        if (entry != null) {
            return entry.getChunkAtLeast(ChunkStep.FEATURES);
        }
        return null;
    }

    @Override
    public IChunk getExistingChunk(int x, int z, ChunkStep step) {
        if (Thread.currentThread() != this.mainThread) {
            return this.loadExistingChunk(x, z, step);
        }

        IChunk cached = this.fastCache.get(x, z, step);
        if (cached != null) {
            return cached;
        }

        IChunk chunk = this.loadExistingChunk(x, z, step);
        this.fastCache.put(x, z, step, chunk);

        return chunk;
    }

    @Override
    public IChunk getAnyExistingChunk(int x, int z) {
        ChunkEntry entry = this.getChunkEntry(x, z);
        if (entry != null) {
            return entry.getChunk();
        }
        return null;
    }

    @Nullable
    private IChunk loadExistingChunk(int x, int z, ChunkStep step) {
        ChunkEntry entry = this.getChunkEntry(x, z);
        return this.getExistingChunkFor(entry, step);
    }

    @Nullable
    private IChunk getExistingChunkFor(@Nullable ChunkEntry entry, ChunkStep step) {
        if (entry != null && entry.isValidAs(step)) {
            return entry.getChunkForStep(step);
        }
        return null;
    }

    /**
     * @reason replace with implementation that will not return true for partially loaded chunks
     * @author gegy1000
     */
    @Overwrite
    public boolean chunkExists(int x, int z) {
        return this.getExistingChunk(x, z, ChunkStep.FULL) != null;
    }

    @Override
    public CompletableFuture<IChunk> getOrCreateChunkAsync(int x, int z, ChunkStep step) {
        CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> future;

        if (Thread.currentThread() != this.mainThread) {
            future = CompletableFuture.supplyAsync(() -> this.createChunk(x, z, step), this.executor)
                    .thenCompose(Function.identity());
        } else {
            future = this.createChunk(x, z, step);
        }

        return future.thenApply(result -> result.map(
                chunk -> chunk,
                unloaded -> {
                    throw new IllegalStateException("Chunk not there when requested: " + unloaded);
                })
        );
    }

    private CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> createChunk(int x, int z, ChunkStep step) {
        ChunkEntry entry = this.getChunkEntry(x, z);

        ChunkPos pos = new ChunkPos(x, z);
        int level = getLevelForStep(step);
        System.out.println("aa");
        this.ticketManager.registerWithLevel(TicketType.UNKNOWN, pos, level, pos);
        System.out.println("bb");

        while (entry == null || !entry.isValidAs(step)) {
            IProfiler profiler = this.world.getProfiler();
            profiler.startSection("chunkLoad");

            // tick the ticket manager to propagate any changes and reacquire the chunk entry
            this.func_217235_l();
            entry = this.getChunkEntry(x, z);

            profiler.endSection();

            if (entry == null || !entry.isValidAs(step)) {
                throw new IllegalStateException("No chunk entry after ticket has been added");
            }
        }
        System.out.println("cc");

        ChunkController controller = (ChunkController) this.chunkManager;
        return controller.getChunkAs(entry, step).asVanilla();
    }

    @Override
    public boolean shouldChunkExist(int x, int z, ChunkStep step) {
        ChunkEntry entry = this.getChunkEntry(x, z);
        return entry != null && entry.getChunkLevel() <= getLevelForStep(step);
    }

    private static int getLevelForStep(ChunkStep step) {
        return ChunkLevelTracker.FULL_LEVEL + ChunkStep.getDistanceFromFull(step);
    }

    @Nullable
    private ChunkEntry getChunkEntry(int x, int z) {
        return (ChunkEntry) this.func_217213_a(ChunkPos.asLong(x, z));
    }

    @Nullable
    private ChunkEntry getChunkEntry(long pos) {
        return (ChunkEntry) this.func_217213_a(pos);
    }

    /**
     * @reason direct logic to {@link ChunkEntry} and avoid allocation
     * @author gegy1000
     */
    @Overwrite
    public boolean isChunkLoaded(Entity entity) {
        ChunkEntry entry = this.getChunkEntry(MathHelper.floor(entity.getPosX()) >> 4, MathHelper.floor(entity.getPosZ()) >> 4);
        return entry != null && entry.isTickingEntities();
    }

    /**
     * @reason direct logic to {@link ChunkEntry} and avoid allocation
     * @author gegy1000
     */
    @Overwrite
    public boolean isChunkLoaded(ChunkPos pos) {
        ChunkEntry entry = this.getChunkEntry(pos.asLong());
        return entry != null && entry.isTickingEntities();
    }

    /**
     * @reason direct logic to {@link ChunkEntry} and avoid allocation
     * @author gegy1000
     */
    @Overwrite
    public boolean canTick(BlockPos pos) {
        ChunkEntry entry = this.getChunkEntry(pos.getX() >> 4, pos.getZ() >> 4);
        return entry != null && entry.isTicking();
    }

    @Shadow
    protected abstract boolean func_217235_l();

    @Shadow
    @Nullable
    protected abstract ChunkHolder func_217213_a(long pos);
}

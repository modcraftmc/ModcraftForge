package common.mixins.tictacs.chunk_ticking;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import net.gegy1000.tictacs.chunk.ChunkAccess;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.minecraft.profiler.IProfiler;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.*;
import net.minecraft.world.spawner.WorldEntitySpawner;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(value = ServerChunkProvider.class, priority = 999)
public abstract class ServerChunkManagerMixin {
    @Shadow
    @Final
    public ChunkManager chunkManager;
    @Shadow
    @Final
    private ServerWorld world;
    @Shadow
    private long lastGameTime;
    @Shadow
    private WorldEntitySpawner.EntityDensityManager field_241097_p_;
    @Shadow
    @Final
    private TicketManager ticketManager;
    @Shadow
    private boolean spawnPassives;
    @Shadow
    private boolean spawnHostiles;

    private ChunkAccess primaryChunks;
    private final List<ChunkEntry> tickingChunks = new ArrayList<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(ServerWorld p_i232603_1_, SaveFormat.LevelSave p_i232603_2_, DataFixer p_i232603_3_, TemplateManager p_i232603_4_, Executor p_i232603_5_, ChunkGenerator p_i232603_6_, int p_i232603_7_, boolean p_i232603_8_, IChunkStatusListener p_i232603_9_, Supplier<DimensionSavedDataManager> p_i232603_10_, CallbackInfo ci) {
        this.primaryChunks = ((ChunkController) this.chunkManager).getMap().primary();
    }

    /**
     * @reason optimize chunk ticking and iteration logic
     * @author gegy1000
     */
    @Overwrite
    private void tickChunks() {
        long time = this.world.getGameTime();

        long timeSinceSpawn = time - this.lastGameTime;
        this.lastGameTime = time;

        boolean doMobSpawning = this.world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING);
        boolean spawnMobs = doMobSpawning && (this.spawnHostiles || this.spawnPassives);

        if (!this.world.isDebug()) {
            IProfiler profiler = this.world.getProfiler();
            profiler.startSection("pollingChunks");

            this.flushChunkUpgrades();

            this.field_241097_p_ = spawnMobs ? this.setupSpawnInfo(this.ticketManager.getSpawningChunksCount()) : null;
            this.tickChunks(timeSinceSpawn, this.field_241097_p_);

            if (doMobSpawning) {
                profiler.startSection("customSpawners");
                this.world.func_241123_a_(this.spawnHostiles, this.spawnPassives);
                profiler.endSection();
            }

            profiler.endSection();
        }

        this.chunkManager.tickEntityTracker();
    }

    private void flushChunkUpgrades() {
        ChunkController controller = (ChunkController) this.chunkManager;
        Collection<ChunkEntry> trackableChunks = controller.getMap().getTickingMaps().getTrackableEntries();

        IProfiler profiler = this.world.getProfiler();
        profiler.startSection("broadcast");

        for (ChunkEntry entry : trackableChunks) {
            Chunk worldChunk = entry.getChunkIfComplete();
            if (worldChunk != null) {
                entry.sendChanges(worldChunk);
            }
        }

        profiler.endSection();
    }

    private void tickChunks(long timeSinceSpawn, WorldEntitySpawner.EntityDensityManager spawnInfo) {
        ChunkController controller = (ChunkController) this.chunkManager;

        List<ChunkEntry> tickingChunks = this.collectTickingChunks(controller);
        if (!tickingChunks.isEmpty()) {
            this.tickChunks(timeSinceSpawn, spawnInfo, tickingChunks);
        }

        this.tickingChunks.clear();
    }

    private void tickChunks(long timeSinceSpawn, WorldEntitySpawner.EntityDensityManager spawnInfo, List<ChunkEntry> chunks) {
        boolean spawnAnimals = this.world.getGameTime() % 400 == 0L;
        int tickSpeed = this.world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED);

        for (ChunkEntry entry : chunks) {
            Chunk worldChunk = entry.getChunkIfComplete();

            if (worldChunk != null && entry.isChunkTickable()) {
                worldChunk.setInhabitedTime(worldChunk.getInhabitedTime() + timeSinceSpawn);

                if (spawnInfo != null && this.world.getWorldBorder().contains(entry.getPosition())) {
                    WorldEntitySpawner.func_234979_a_(this.world, worldChunk, spawnInfo, this.spawnPassives, this.spawnHostiles, spawnAnimals);
                }

                this.world.tickEnvironment(worldChunk, tickSpeed);
            }
        }
    }

    private WorldEntitySpawner.EntityDensityManager setupSpawnInfo(int spawnChunkCount) {
        IProfiler profiler = this.world.getProfiler();
        profiler.startSection("naturalSpawnCount");

        WorldEntitySpawner.EntityDensityManager spawnInfo = WorldEntitySpawner.func_234964_a_(spawnChunkCount, this.world.func_241136_z_(), this::ifChunkLoaded);

        profiler.endSection();

        return spawnInfo;
    }

    private List<ChunkEntry> collectTickingChunks(ChunkController controller) {
        Collection<ChunkEntry> entries = controller.getMap().getTickingMaps().getTickableEntries();
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChunkEntry> tickingChunks = this.tickingChunks;
        tickingChunks.clear();
        tickingChunks.addAll(entries);

        Collections.shuffle(tickingChunks);

        return tickingChunks;
    }

    @Unique
    private void ifChunkLoaded(long pos, Consumer<Chunk> consumer) {
        ChunkEntry entry = this.primaryChunks.getEntry(pos);
        if (entry != null) {
            Either<Chunk, ChunkHolder.IChunkLoadingError> accessible = entry.getBorderFuture().getNow(null);
            if (accessible == null) {
                return;
            }

            accessible.ifLeft(consumer);
        }
    }
}

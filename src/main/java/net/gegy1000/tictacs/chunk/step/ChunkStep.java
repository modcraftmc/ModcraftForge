package net.gegy1000.tictacs.chunk.step;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.gegy1000.justnow.future.Future;
import net.gegy1000.justnow.tuple.Unit;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.ChunkLevelTracker;
import net.gegy1000.tictacs.chunk.ChunkLockType;
import net.gegy1000.tictacs.chunk.future.FutureHandle;
import net.gegy1000.tictacs.compatibility.TicTacsCompatibility;
import net.gegy1000.tictacs.config.TicTacsConfig;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

// TODO: separate loading chunk steps and generation chunk steps
//       allow arbitrary tasks to be attached to steps
public final class ChunkStep {
    private static final EnumSet<Heightmap.Type> REQUIRED_FEATURE_HEIGHTMAPS = EnumSet.of(
            Heightmap.Type.MOTION_BLOCKING,
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
            Heightmap.Type.OCEAN_FLOOR,
            Heightmap.Type.WORLD_SURFACE
    );

    public static final List<ChunkStep> STEPS = new ArrayList<>();

    public static final ChunkStep EMPTY = new ChunkStep("empty")
            .includes(ChunkStatus.EMPTY)
            .locks(ChunkLockType.EARLY_GENERATION)
            .requires(ChunkRequirements.none());

    public static final ChunkStep STRUCTURE_STARTS = new ChunkStep("structure_starts")
            .includes(ChunkStatus.STRUCTURE_STARTS)
            .locks(ChunkLockType.EARLY_GENERATION)
            .requires(ChunkRequirements.from(ChunkStep.EMPTY))
            .upgradeSync(ChunkStep::addStructureStarts);

    public static final ChunkStep SURFACE = new ChunkStep("surface")
            .includes(
                    ChunkStatus.STRUCTURE_REFERENCES, ChunkStatus.BIOMES,
                    ChunkStatus.NOISE, ChunkStatus.SURFACE,
                    ChunkStatus.CARVERS, ChunkStatus.LIQUID_CARVERS
            )
            .locks(ChunkLockType.LATE_GENERATION)
            .requires(
                    ChunkRequirements.from(ChunkStep.STRUCTURE_STARTS)
                            .read(ChunkStep.STRUCTURE_STARTS, 8)
            )
            .upgradeSync(ChunkStep::generateSurface);

    public static final ChunkStep FEATURES = new ChunkStep("features")
            .includes(ChunkStatus.FEATURES)
            .requires(
                    ChunkRequirements.from(ChunkStep.SURFACE)
                            // Feature gen radius is controlled by the config, it's usually 2 but can be higher.
                            .write(ChunkStep.SURFACE, TicTacsConfig.get().featureGenerationRadius)
                            .read(ChunkStep.STRUCTURE_STARTS, 8)
            )
            .locks(ChunkLockType.LATE_GENERATION)
            .upgradeAsync(ChunkStep::addFeatures)
            .loadAsync(ChunkStep::loadFeatures);

    public static final ChunkStep LIGHTING = new ChunkStep("lighting")
            .includes(ChunkStatus.LIGHT)
            .requires(
                    ChunkRequirements.from(ChunkStep.FEATURES)
                            .read(ChunkStep.FEATURES, 1)
                            .require(ChunkStep.FEATURES, 1 + TicTacsConfig.get().featureGenerationRadius)
            )
            .locks(ChunkLockType.FINALIZATION)
            .acquire(ChunkStep::acquireLight)
            .release(ChunkStep::releaseLight)
            .upgradeAsync(ctx -> ChunkStep.lightChunk(ctx, false))
            .loadAsync(ctx -> ChunkStep.lightChunk(ctx, true));

    public static final ChunkStep FULL = new ChunkStep("full")
            .includes(ChunkStatus.SPAWN, ChunkStatus.HEIGHTMAPS, ChunkStatus.FULL)
            .requires(ChunkRequirements.from(ChunkStep.LIGHTING))
            .locks(ChunkLockType.FINALIZATION)
            .upgradeAsync(ctx -> {
                ChunkStep.addEntities(ctx);
                return ChunkStep.makeFull(ctx);
            })
            .loadAsync(ChunkStep::makeFull);

    public static final ChunkStep GENERATION = ChunkStep.LIGHTING.getPrevious();
    public static final ChunkStep MIN_WITH_LOAD_TASK = TicTacsCompatibility.PHOSPHOR_LOADED ? ChunkStep.FEATURES : ChunkStep.LIGHTING;

    private static final ChunkStep[] STATUS_TO_STEP;

    private static final int[] STEP_TO_RADIUS;

    private static final int MAX_DISTANCE;
    private static final int[] STEP_TO_DISTANCE;
    private static final ChunkStep[] DISTANCE_TO_STEP;

    private final int index;
    private final String name;
    private ChunkStatus[] statuses = new ChunkStatus[0];
    private ChunkLockType lock;
    private ChunkRequirements requirements = ChunkRequirements.none();

    private AsyncTask upgradeTask = AsyncTask.noop();
    private AsyncTask loadTask = AsyncTask.noop();
    private Acquire acquireTask;
    private Release releaseTask;

    ChunkStep(String name) {
        int index = STEPS.size();
        STEPS.add(this);

        this.index = index;
        this.name = name;
    }

    ChunkStep includes(ChunkStatus... statuses) {
        this.statuses = statuses;
        return this;
    }

    ChunkStep locks(ChunkLockType lock) {
        this.lock = lock;
        return this;
    }

    ChunkStep requires(ChunkRequirements requirements) {
        this.requirements = requirements;
        return this;
    }

    ChunkStep upgradeAsync(AsyncTask task) {
        this.upgradeTask = task;
        return this;
    }

    ChunkStep upgradeSync(SyncTask task) {
        this.upgradeTask = AsyncTask.from(task);
        return this;
    }

    ChunkStep loadAsync(AsyncTask task) {
        this.loadTask = task;
        return this;
    }

    ChunkStep acquire(Acquire acquire) {
        this.acquireTask = acquire;
        return this;
    }

    ChunkStep release(Release task) {
        this.releaseTask = task;
        return this;
    }

    public Future<IChunk> runUpgrade(ChunkStepContext ctx) {
        return this.upgradeTask.run(ctx);
    }

    public Future<IChunk> runLoad(ChunkStepContext ctx) {
        return this.loadTask.run(ctx);
    }

    public ChunkRequirements getRequirements() {
        return this.requirements;
    }

    public ChunkLockType getLock() {
        return this.lock;
    }

    public Acquire getAcquireTask() {
        return this.acquireTask;
    }

    public Release getReleaseTask() {
        return this.releaseTask;
    }

    public ChunkStatus[] getStatuses() {
        return this.statuses;
    }

    public ChunkStatus getMaximumStatus() {
        return this.statuses[this.statuses.length - 1];
    }

    public int getIndex() {
        return this.index;
    }

    public boolean greaterOrEqual(ChunkStep step) {
        return step == null || this.index >= step.index;
    }

    public boolean lessOrEqual(ChunkStep step) {
        return step != null && this.index <= step.index;
    }

    public boolean greaterThan(ChunkStep step) {
        return step == null || this.index > step.index;
    }

    public boolean lessThan(ChunkStep step) {
        return step != null && this.index < step.index;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public ChunkStep getPrevious() {
        return byIndex(this.index - 1);
    }

    public ChunkStep getNext() {
        return byIndex(this.index + 1);
    }

    public static ChunkStep min(ChunkStep a, ChunkStep b) {
        if (a == null || b == null) return null;

        return a.index < b.index ? a : b;
    }

    public static ChunkStep max(ChunkStep a, ChunkStep b) {
        if (a == null) return b;
        if (b == null) return a;

        return a.index > b.index ? a : b;
    }

    public static ChunkStep byIndex(int index) {
        if (index < 0 || index >= STEPS.size()) {
            return null;
        }
        return STEPS.get(index);
    }

    public static ChunkStep byStatus(ChunkStatus status) {
        return STATUS_TO_STEP[status.ordinal()];
    }

    public static ChunkStep byFullStatus(ChunkStatus status) {
        ChunkStep step = byStatus(status);
        if (status == step.getMaximumStatus()) {
            return step;
        } else {
            return step.getPrevious();
        }
    }

    public static int getRequiredRadius(ChunkStep step) {
        return STEP_TO_RADIUS[step.getIndex()];
    }

    public static int getDistanceFromFull(ChunkStep step) {
        return STEP_TO_DISTANCE[step.getIndex()];
    }

    public static ChunkStep byDistanceFromFull(int distance) {
        if (distance < 0) return FULL;
        if (distance >= DISTANCE_TO_STEP.length) return EMPTY;

        return DISTANCE_TO_STEP[distance];
    }

    public static int getMaxDistance() {
        return MAX_DISTANCE;
    }

    static {
        // initialize status -> step mapping
        List<ChunkStatus> statuses = ChunkStatus.getAll();
        STATUS_TO_STEP = new ChunkStep[statuses.size()];

        for (ChunkStep step : STEPS) {
            for (ChunkStatus status : step.statuses) {
                STATUS_TO_STEP[status.ordinal()] = step;
            }
        }

        StepKernelResolver.Results results = new StepKernelResolver(STEPS).resolve();

        MAX_DISTANCE = results.maxDistance;
        STEP_TO_RADIUS = results.stepToRadius;
        DISTANCE_TO_STEP = results.distanceToStep;
        STEP_TO_DISTANCE = results.stepToDistance;
    }

    private static IChunk addStructureStarts(ChunkStepContext ctx) {
        ServerWorld world = ctx.world;
        DimensionGeneratorSettings options = world.getServer().getServerConfiguration().getDimensionGeneratorSettings();
        if (options.doesGenerateFeatures()) {
            ctx.generator.func_242707_a(world.func_241828_r(), world.func_241112_a_(), ctx.chunk, ctx.structures, world.getSeed());
        }
        return ctx.chunk;
    }

    private static IChunk generateSurface(ChunkStepContext ctx) {
        ChunkGenerator generator = ctx.generator;
        ServerWorld world = ctx.world;
        IChunk chunk = ctx.chunk;

        WorldGenRegion region = ctx.asRegion();
        StructureManager structureAccessor = ctx.asStructureAccessor();

        generator.func_235953_a_(region, structureAccessor, chunk);
        trySetStatus(chunk, ChunkStatus.STRUCTURE_REFERENCES);

        generator.func_242706_a(world.func_241828_r().getRegistry(Registry.BIOME_KEY), chunk);
        trySetStatus(chunk, ChunkStatus.BIOMES);

        generator.func_230352_b_(region, structureAccessor, chunk);
        trySetStatus(chunk, ChunkStatus.NOISE);

        generator.generateSurface(region, chunk);
        trySetStatus(chunk, ChunkStatus.SURFACE);

        generator.func_230350_a_(world.getSeed(), world.getBiomeManager(), chunk, GenerationStage.Carving.AIR);
        trySetStatus(chunk, ChunkStatus.CARVERS);

        generator.func_230350_a_(world.getSeed(), world.getBiomeManager(), chunk, GenerationStage.Carving.LIQUID);
        trySetStatus(chunk, ChunkStatus.LIQUID_CARVERS);

        return chunk;
    }

    private static Future<IChunk> addFeatures(ChunkStepContext ctx) {
        ChunkPrimer proto = (ChunkPrimer) ctx.chunk;
        proto.setLightManager(ctx.lighting);

        Heightmap.updateChunkHeightmaps(ctx.chunk, REQUIRED_FEATURE_HEIGHTMAPS);

        WorldGenRegion region = ctx.asRegion();
        ctx.generator.func_230351_a_(region, ctx.world.func_241112_a_().getStructureManager(region));

        return TicTacsCompatibility.afterFeaturesStep(ctx);
    }

    private static Future<IChunk> loadFeatures(ChunkStepContext ctx) {
        return TicTacsCompatibility.afterFeaturesStep(ctx);
    }

    private static Future<IChunk> lightChunk(ChunkStepContext ctx, boolean load) {
        trySetStatus(ctx.chunk, ChunkStatus.LIGHT);

        TicketManager ticketManager = ctx.controller.getTicketManager();

        FutureHandle<IChunk> handle = new FutureHandle<>();

        ChunkPos pos = ctx.entry.getPosition();
        ctx.controller.spawnOnMainThread(ctx.entry, () -> {
            ticketManager.registerWithLevel(TicketType.LIGHT, pos, ChunkLevelTracker.LIGHT_TICKET_LEVEL, pos);

            ctx.lighting.lightChunk(ctx.chunk, load && ctx.chunk.hasLight()).thenAccept(handle::complete);
        });

        return handle;
    }

    private static Future<Unit> acquireLight(ChunkController controller) {
        return controller.getUpgrader().lightingThrottler.acquireAsync();
    }

    private static void releaseLight(ChunkController controller) {
        controller.getUpgrader().lightingThrottler.release();
    }

    private static void addEntities(ChunkStepContext ctx) {
        WorldGenRegion region = ctx.asRegion();
        ctx.generator.func_230354_a_(region);
    }

    private static Future<IChunk> makeFull(ChunkStepContext ctx) {
        FutureHandle<IChunk> handle = new FutureHandle<>();

        ctx.controller.spawnOnMainThread(ctx.entry, () -> {
            LongSet loadedChunks = ctx.controller.asTacs().loadedPositions;

            Chunk worldChunk = ctx.entry.finalizeChunk(ctx.world, loadedChunks::add);
            handle.complete(worldChunk);
        });

        return handle;
    }

    public static void trySetStatus(IChunk chunk, ChunkStatus status) {
        if (chunk instanceof ChunkPrimer) {
            ChunkPrimer protoChunk = (ChunkPrimer) chunk;
            if (!protoChunk.getStatus().isAtLeast(status)) {
                protoChunk.setStatus(status);
            }
        }
    }

    public interface AsyncTask {
        static AsyncTask noop() {
            return ctx -> Future.ready(ctx.chunk);
        }

        static AsyncTask from(SyncTask task) {
            return ctx -> Future.ready(task.run(ctx));
        }

        Future<IChunk> run(ChunkStepContext ctx);
    }

    public interface SyncTask {
        static SyncTask noop() {
            return ctx -> ctx.chunk;
        }

        IChunk run(ChunkStepContext ctx);
    }

    public interface Acquire {
        Future<Unit> acquire(ChunkController controller);
    }

    public interface Release {
        void release(ChunkController controller);
    }
}

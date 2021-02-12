package common.mixins.tictacs.unblocking;

import net.gegy1000.tictacs.AsyncChunkAccess;
import net.gegy1000.tictacs.NonBlockingWorldAccess;
import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements NonBlockingWorldAccess, AsyncChunkAccess {
    @Shadow
    @Final
    private ServerChunkProvider field_241102_C_;

    protected ServerWorldMixin(ISpawnWorldInfo worldInfo, RegistryKey<World> dimension, DimensionType dimensionType, Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed) {
        super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
    }


    @Override
    public BlockState getBlockStateIfLoaded(BlockPos pos) {
        if (isOutsideBuildHeight(pos)) {
            return Blocks.VOID_AIR.getDefaultState();
        }

        IChunk chunk = this.getExistingChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStep.FEATURES);
        if (chunk != null) {
            return chunk.getBlockState(pos);
        } else {
            return Blocks.AIR.getDefaultState();
        }
    }

    @Override
    public FluidState getFluidStateIfLoaded(BlockPos pos) {
        if (isOutsideBuildHeight(pos)) {
            return Fluids.EMPTY.getDefaultState();
        }

        IChunk chunk = this.getExistingChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStep.FEATURES);
        if (chunk != null) {
            return chunk.getFluidState(pos);
        } else {
            return Fluids.EMPTY.getDefaultState();
        }
    }

    @Override
    public int getHeight(Heightmap.Type heightmap, int x, int z) {
        if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000) {
            return this.getSeaLevel() + 1;
        }

        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (this.shouldChunkExist(chunkX, chunkZ, ChunkStep.FEATURES)) {
            IChunk chunk = this.getChunk(chunkX, chunkZ, ChunkStatus.FEATURES);
            return chunk.getTopBlockY(heightmap, x & 15, z & 15) + 1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean chunkExists(int x, int z) {
        return this.getExistingChunk(x, z, ChunkStep.FULL) != null;
    }

    @Override
    public IChunk getExistingChunk(int x, int z, ChunkStep step) {
        return ((AsyncChunkAccess) this.field_241102_C_).getExistingChunk(x, z, step);
    }

    @Override
    public IChunk getAnyExistingChunk(int chunkX, int chunkZ) {
        return ((AsyncChunkAccess) this.field_241102_C_).getAnyExistingChunk(chunkX, chunkZ);
    }

    @Override
    public CompletableFuture<IChunk> getOrCreateChunkAsync(int x, int z, ChunkStep step) {
        return ((AsyncChunkAccess) this.field_241102_C_).getOrCreateChunkAsync(x, z, step);
    }

    @Override
    public boolean shouldChunkExist(int x, int z, ChunkStep step) {
        return ((AsyncChunkAccess) this.field_241102_C_).shouldChunkExist(x, z, step);
    }
}

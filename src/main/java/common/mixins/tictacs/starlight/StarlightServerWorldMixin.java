package common.mixins.tictacs.starlight;

import ca.spottedleaf.starlight.common.world.ExtendedWorld;
import net.gegy1000.tictacs.AsyncChunkAccess;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Mixin(value = ServerWorld.class, priority = 1001)
public abstract class StarlightServerWorldMixin extends World implements ExtendedWorld {
    @Shadow
    @Final
    private ServerChunkProvider field_241102_C_;

    protected StarlightServerWorldMixin(ISpawnWorldInfo worldInfo, RegistryKey<World> dimension, DimensionType dimensionType, Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed) {
        super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
    }


    // these implementations are not strictly necessary, but they optimize starlight's chunk queries
    @Override
    public Chunk getChunkAtImmediately(int x, int z) {
        return this.field_241102_C_.getChunkNow(x, z);
    }

    @Override
    public IChunk getAnyChunkImmediately(int x, int z) {
        return ((AsyncChunkAccess) this.field_241102_C_).getAnyExistingChunk(x, z);
    }
}

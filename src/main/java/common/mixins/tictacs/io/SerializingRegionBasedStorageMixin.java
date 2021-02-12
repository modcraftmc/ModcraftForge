package common.mixins.tictacs.io;

import com.mojang.serialization.DynamicOps;
import net.gegy1000.tictacs.AsyncChunkIo;
import net.gegy1000.tictacs.AsyncRegionStorageIo;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.IOWorker;
import net.minecraft.world.chunk.storage.RegionSectionCache;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(RegionSectionCache.class)
public abstract class SerializingRegionBasedStorageMixin implements AsyncRegionStorageIo, AsyncChunkIo {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private IOWorker field_227173_b_;

    @Shadow
    protected abstract <T> void func_235992_a_(ChunkPos pos, DynamicOps<T> dynamicOps, T data);

    @Override
    public CompletableFuture<Void> loadDataAtAsync(ChunkPos pos, Executor mainThreadExecutor) {
        return this.getNbtAsync(pos).thenAcceptAsync(tag -> {
            this.func_235992_a_(pos, NBTDynamicOps.INSTANCE, tag);
        }, mainThreadExecutor);
    }

    @Override
    public CompletableFuture<CompoundNBT> getNbtAsync(ChunkPos pos) {
        return ((AsyncChunkIo) this.field_227173_b_).getNbtAsync(pos).handle((tag, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Error reading chunk {} data from disk", pos, throwable);
                return null;
            }
            return tag;
        });
    }
}

package common.mixins.tictacs.io;

import com.mojang.datafixers.util.Either;
import net.gegy1000.tictacs.AsyncChunkIo;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.IOWorker;
import net.minecraft.world.chunk.storage.RegionFileCache;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mixin(IOWorker.class)
public abstract class StorageIoWorkerMixin implements AsyncChunkIo {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private RegionFileCache field_227084_e_;

    @Shadow
    @Final
    private Map<ChunkPos, IOWorker.Entry> field_227085_f_;

    @Shadow
    protected abstract <T> CompletableFuture<T> func_235975_a_(Supplier<Either<T, Exception>> supplier);

    @Override
    public CompletableFuture<CompoundNBT> getNbtAsync(ChunkPos pos) {
        return this.func_235975_a_(() -> {
            IOWorker.Entry result = this.field_227085_f_.get(pos);
            if (result == null) {
                return this.loadNbtAt(pos);
            }
            return Either.left(result.field_227113_a_);
        });
    }

    public Either<CompoundNBT, Exception> loadNbtAt(ChunkPos pos) {
        try {
            CompoundNBT compoundTag = this.field_227084_e_.readChunk(pos);
            return Either.left(compoundTag);
        } catch (Exception e) {
            LOGGER.warn("Failed to read chunk {}", pos, e);
            return Either.right(e);
        }
    }
}

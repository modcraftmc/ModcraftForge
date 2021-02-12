package common.mixins.tictacs.io;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import net.gegy1000.tictacs.AsyncChunkIo;
import net.gegy1000.tictacs.AsyncRegionStorageIo;
import net.gegy1000.tictacs.chunk.io.ChunkData;
import net.minecraft.crash.ReportedException;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

@Mixin(ChunkManager.class)
public abstract class ThreadedAnvilChunkStorageMixin extends ChunkLoader implements AsyncChunkIo {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private ServerWorld world;
    @Shadow
    @Final
    private PointOfInterestManager pointOfInterestManager;
    @Shadow
    @Final
    private TemplateManager templateManager;
    @Shadow
    @Final
    private Supplier<DimensionSavedDataManager> field_219259_m;
    @Shadow
    @Final
    private ThreadTaskExecutor<Runnable> mainThread;

    private ThreadedAnvilChunkStorageMixin(File file, DataFixer dataFixer, boolean dsync) {
        super(file, dataFixer, dsync);
    }

    /**
     * @reason avoid blocking the main thread to load chunk
     * @author gegy1000
     */
    @Overwrite
    public CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> chunkLoad(ChunkPos pos) {
        return this.getUpdatedChunkTagAsync(pos)
                .thenApplyAsync(tag -> this.deserializeChunkData(pos, tag), Util.getServerExecutor())
                .handleAsync((data, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof CompletionException) {
                            throwable = throwable.getCause();
                        }
                        LOGGER.error("Couldn't load chunk {}", pos, throwable);
                    }

                    return this.createChunkFromData(pos, data);
                }, this.mainThread);
    }

    private Either<IChunk, ChunkHolder.IChunkLoadingError> createChunkFromData(ChunkPos pos, ChunkData data) {
        if (data != null) {
            this.world.getProfiler().func_230035_c_("chunkLoad");

            try {
                IChunk chunk = data.createChunk(this.world, this.templateManager, this.pointOfInterestManager);
                chunk.setLastSaveTime(this.world.getGameTime());

                this.func_241088_a_(pos, chunk.getStatus().getType());
                return Either.left(chunk);
            } catch (ReportedException crash) {
                Throwable cause = crash.getCause();
                if (!(cause instanceof IOException)) {
                    this.func_241089_g_(pos);
                    throw crash;
                }

                LOGGER.error("Couldn't load chunk {}", pos, crash);
            } catch (Exception e) {
                LOGGER.error("Couldn't load chunk {}", pos, e);
            }
        }

        this.func_241089_g_(pos);
        return Either.left(new ChunkPrimer(pos, UpgradeData.EMPTY));
    }

    private ChunkData deserializeChunkData(ChunkPos pos, CompoundNBT tag) {
        if (tag == null) {
            return null;
        }

        if (!tag.contains("Level", Constants.NBT.TAG_COMPOUND) || !tag.getCompound("Level").contains("Status", Constants.NBT.TAG_STRING)) {
            LOGGER.error("Chunk file at {} is missing level data, skipping", pos);
            return null;
        }

        return ChunkData.deserialize(pos, tag);
    }

    private CompletableFuture<CompoundNBT> getUpdatedChunkTagAsync(ChunkPos pos) {
        CompletableFuture<CompoundNBT> chunkTag = this.getNbtAsync(pos)
                .thenCompose(tag -> {
                    if (tag != null && getDataVersion(tag) < SharedConstants.getVersion().getWorldVersion()) {
                        // TODO: ideally we don't need to datafix chunks on the main thread
                        return CompletableFuture.supplyAsync(() -> {
                            return this.func_235968_a_(this.world.getDimensionKey(), this.field_219259_m, tag);
                        }, this.mainThread);
                    }

                    return CompletableFuture.completedFuture(tag);
                });

        CompletableFuture<Void> loadPoi = ((AsyncRegionStorageIo) this.pointOfInterestManager).loadDataAtAsync(pos, this.mainThread);

        return chunkTag.thenCombine(loadPoi, (tag, v) -> tag);
    }

    @Shadow
    protected abstract void func_241089_g_(ChunkPos chunkPos);

    @Shadow
    protected abstract byte func_241088_a_(ChunkPos chunkPos, ChunkStatus.Type chunkType);
}

package common.mixins.krypton.flushconsolidation;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.steinborn.krypton.network.util.AutoFlushUtil;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PlayerGenerationTracker;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Mixes into various methods in {@code ThreadedAnvilChunkStorage} to utilize flush consolidation for sending chunks
 * all at once to the client. Helpful for heavy server activity or flying very quickly.
 */
@Mixin(value = ChunkManager.class, priority = 1020)
public abstract class ThreadedAnvilChunkStorageMixin {
    @Shadow @Final private Int2ObjectMap<ChunkManager.EntityTracker> entities;
    @Shadow @Final private PlayerGenerationTracker playerGenerationTracker;
    @Shadow @Final private ServerWorld world;
    @Shadow @Final private ChunkManager.ProxyTicketManager ticketManager;
    @Shadow private int viewDistance;

    @Shadow protected abstract boolean cannotGenerateChunks(ServerPlayerEntity player);

    @Shadow protected abstract SectionPos func_223489_c(ServerPlayerEntity serverPlayerEntity);

    @Shadow
    private static int getChunkDistance(ChunkPos pos, int x, int z) {
        throw new AssertionError("pedantic");
    }

    @Shadow public abstract void sendChunkData(ServerPlayerEntity player, IPacket<?>[] packets, Chunk chunk);

    @Shadow @Nullable
    protected abstract ChunkHolder func_219219_b(long pos);

    /**
     * @author Andrew Steinborn
     * @reason Add support for flush consolidation
     */
    @Overwrite
    public void updatePlayerPosition(ServerPlayerEntity player) {
        for (ChunkManager.EntityTracker entityTracker : this.entities.values()) {
            if (entityTracker.entity == player) {
                entityTracker.updateTrackingState(this.world.getPlayers());
            } else {
                entityTracker.updateTrackingState(player);
            }
        }

        SectionPos oldPos = player.getManagedSectionPos();
        SectionPos newPos = SectionPos.from(player);
        boolean isWatchingWorld = this.playerGenerationTracker.canGeneratePlayer(player);
        boolean noChunkGen = this.cannotGenerateChunks(player);
        boolean movedSections = !oldPos.equals(newPos);

        if (movedSections || isWatchingWorld != noChunkGen) {
            this.func_223489_c(player);

            if (!isWatchingWorld) {
                this.ticketManager.removePlayer(oldPos, player);
            }

            if (!noChunkGen) {
                this.ticketManager.updatePlayerPosition(newPos, player);
            }

            if (!isWatchingWorld && noChunkGen) {
                this.playerGenerationTracker.disableGeneration(player);
            }

            if (isWatchingWorld && !noChunkGen) {
                this.playerGenerationTracker.enableGeneration(player);
            }

            long oldChunkPos = ChunkPos.asLong(oldPos.getX(), oldPos.getZ());
            long newChunkPos = ChunkPos.asLong(newPos.getX(), newPos.getZ());
            this.playerGenerationTracker.updatePlayerPosition(oldChunkPos, newChunkPos, player);

            // If the player is in the same world as this tracker, we should send them chunks.
            if (this.world == player.world) {
                this.sendChunks(oldPos, player);
            }
        }
    }

    private void sendChunks(SectionPos oldPos, ServerPlayerEntity player) {
        AutoFlushUtil.setAutoFlush(player, false);

        try {
            int oldChunkX = oldPos.getSectionX();
            int oldChunkZ = oldPos.getSectionZ();

            int newChunkX = MathHelper.floor(player.getPosX()) >> 4;
            int newChunkZ = MathHelper.floor(player.getPosZ()) >> 4;

            if (Math.abs(oldChunkX - newChunkX) <= this.viewDistance * 2 && Math.abs(oldChunkZ - newChunkZ) <= this.viewDistance * 2) {
                int minSendChunkX = Math.min(newChunkX, oldChunkX) - this.viewDistance;
                int maxSendChunkZ = Math.min(newChunkZ, oldChunkZ) - this.viewDistance;
                int q = Math.max(newChunkX, oldChunkX) + this.viewDistance;
                int r = Math.max(newChunkZ, oldChunkZ) + this.viewDistance;

                for (int curX = minSendChunkX; curX <= q; ++curX) {
                    for (int curZ = maxSendChunkZ; curZ <= r; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        boolean inOld = getChunkDistance(chunkPos, oldChunkX, oldChunkZ) <= this.viewDistance;
                        boolean inNew = getChunkDistance(chunkPos, newChunkX, newChunkZ) <= this.viewDistance;
                        this.sendPacketsForChunk(player, chunkPos, new IPacket[2], inOld, inNew);
                    }
                }
            } else {
                for (int curX = oldChunkX - this.viewDistance; curX <= oldChunkX + this.viewDistance; ++curX) {
                    for (int curZ = oldChunkZ - this.viewDistance; curZ <= oldChunkZ + this.viewDistance; ++curZ) {
                        ChunkPos pos = new ChunkPos(curX, curZ);
                        this.sendPacketsForChunk(player, pos, new IPacket[2], true, false);
                    }
                }

                for (int curX = newChunkX - this.viewDistance; curX <= newChunkX + this.viewDistance; ++curX) {
                    for (int curZ = newChunkZ - this.viewDistance; curZ <= newChunkZ + this.viewDistance; ++curZ) {
                        ChunkPos pos = new ChunkPos(curX, curZ);
                        this.sendPacketsForChunk(player, pos, new IPacket[2], false, true);
                    }
                }
            }
        } finally {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }

    protected void sendPacketsForChunk(ServerPlayerEntity player, ChunkPos pos, IPacket<?>[] packets, boolean withinMaxWatchDistance, boolean withinViewDistance) {
        if (withinViewDistance && !withinMaxWatchDistance) {
            ChunkHolder chunkHolder = this.func_219219_b(pos.asLong());
            if (chunkHolder != null) {
                Chunk worldChunk = chunkHolder.getChunkIfComplete();
                if (worldChunk != null) {
                    this.sendChunkData(player, packets, worldChunk);
                }

                DebugPacketSender.sendChuckPos(this.world, pos);
            }
        }

        if (!withinViewDistance && withinMaxWatchDistance) {
            player.sendChunkUnload(pos);
        }
    }

    @Inject(method = "tickEntityTracker", at = @At("HEAD"))
    public void disableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            AutoFlushUtil.setAutoFlush(player, false);
        }
    }

    @Inject(method = "tickEntityTracker", at = @At("RETURN"))
    public void enableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }

}

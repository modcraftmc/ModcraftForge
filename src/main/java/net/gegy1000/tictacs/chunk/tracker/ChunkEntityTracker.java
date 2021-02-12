package net.gegy1000.tictacs.chunk.tracker;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.gegy1000.tictacs.QueuingConnection;
import net.gegy1000.tictacs.chunk.ChunkAccess;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.gegy1000.tictacs.chunk.entry.ChunkEntryTrackers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.TrackedEntity;
import net.minecraft.world.server.ServerWorld;

import java.util.Set;

public final class ChunkEntityTracker {
    private final TrackedEntity entry;
    private final int maxTrackDistance;

    private Set<ServerPlayerEntity> trackingPlayers;

    private ChunkEntry chunkEntry;
    private long chunkPos;

    public ChunkEntityTracker(Entity entity) {
        EntityType<?> type = entity.getType();
        int tickInterval = type.defaultUpdateIntervalSupplier();
        boolean updateVelocity = type.defaultVelocitySupplier();

        this.entry = new TrackedEntity((ServerWorld) entity.world, entity, tickInterval, updateVelocity, this::sendToTracking);
        this.maxTrackDistance = type.defaultTrackingRangeSupplier();
    }

    public Entity getEntity() {
        return this.entry.trackedEntity;
    }

    public boolean tick(ChunkController controller) {
        boolean moved = false;

        long chunkPos = chunkForEntity(this.entry.trackedEntity);
        if (chunkPos != this.chunkPos || this.chunkEntry == null) {
            ChunkAccess chunks = controller.getMap().primary();

            ChunkEntry fromChunkEntry = this.chunkEntry;
            ChunkEntry toChunkEntry = chunks.getEntry(chunkPos);

            this.chunkEntry = toChunkEntry;
            this.chunkPos = chunkPos;

            this.moveChunk(fromChunkEntry, toChunkEntry);
            moved = true;
        }

        this.entry.tick();

        return moved;
    }

    void remove() {
        if (this.chunkEntry != null) {
            this.chunkEntry.getTrackers().removeEntity(this);
            this.chunkEntry = null;
        }

        if (this.trackingPlayers != null) {
            for (ServerPlayerEntity player : this.trackingPlayers) {
                this.entry.untrack(player);
            }
            this.trackingPlayers = null;
        }
    }

    private void moveChunk(ChunkEntry from, ChunkEntry to) {
        if (from != null) {
            this.moveFromChunk(from);
        }

        if (to != null) {
            this.moveToChunk(to);
        }
    }

    private void moveFromChunk(ChunkEntry from) {
        ChunkEntryTrackers trackers = from.getTrackers();
        for (ServerPlayerEntity player : trackers.getTrackingPlayers()) {
            this.updateTrackerUnwatched(player);
        }

        trackers.removeEntity(this);
    }

    private void moveToChunk(ChunkEntry to) {
        ChunkEntryTrackers trackers = to.getTrackers();
        for (ServerPlayerEntity player : trackers.getTrackingPlayers()) {
            this.updateTrackerWatched(player);
        }

        trackers.addEntity(this);
    }

    public void updateTracker(ServerPlayerEntity player) {
        boolean isTracked = this.isTrackedBy(player);
        boolean canTrack = this.canBeTrackedBy(player);

        if (isTracked != canTrack) {
            if (canTrack) {
                this.startTracking(player);
            } else {
                this.stopTracking(player);
            }
        }
    }

    public void updateTrackerWatched(ServerPlayerEntity player) {
        if (!this.isTrackedBy(player) && this.canBeTrackedBy(player)) {
            this.startTracking(player);
        }
    }

    public void updateTrackerUnwatched(ServerPlayerEntity player) {
        if (this.isTrackedBy(player) && !this.canBeTrackedBy(player)) {
            this.stopTracking(player);
        }
    }

    private void startTracking(ServerPlayerEntity player) {
        if (this.trackingPlayers == null) {
            this.trackingPlayers = new ObjectOpenHashSet<>(2, Hash.DEFAULT_LOAD_FACTOR);
        }

        if (this.trackingPlayers.add(player)) {
            this.entry.track(player);
        }
    }

    private void stopTracking(ServerPlayerEntity player) {
        if (this.trackingPlayers != null && this.trackingPlayers.remove(player)) {
            if (this.trackingPlayers.isEmpty()) {
                this.trackingPlayers = null;
            }

            this.entry.untrack(player);
        }
    }

    private boolean isTrackedBy(ServerPlayerEntity player) {
        return this.trackingPlayers != null && this.trackingPlayers.contains(player);
    }

    private boolean canBeTrackedBy(ServerPlayerEntity player) {
        if (player == this.entry.trackedEntity) {
            return false;
        } else if (player.forceSpawn) {
            return true;
        }

        if (this.chunkEntry == null || !this.chunkEntry.getTrackers().isTrackedBy(player)) {
            return false;
        }

        int chunkX = ChunkPos.getX(this.chunkPos);
        int chunkZ = ChunkPos.getZ(this.chunkPos);

        int distance = ChunkTracker.getChunkDistance(player, chunkX, chunkZ);
        return distance < this.getEffectiveTrackDistance();
    }

    public void sendToTrackingAndSelf(IPacket<?> packet) {
        this.sendToTracking(packet);
        this.sendToSelf(packet);
    }

    public void sendToTracking(IPacket<?> packet) {
        if (this.trackingPlayers == null) {
            return;
        }

        if (this.entry.trackedEntity instanceof ServerPlayerEntity) {
            for (ServerPlayerEntity player : this.trackingPlayers) {
                player.connection.sendPacket(packet);
            }
        } else {
            // entity tracker updates are lower priority than players so it should be fine to queue them
            for (ServerPlayerEntity player : this.trackingPlayers) {
                QueuingConnection.enqueueSend(player.connection, packet);
            }
        }
    }

    private void sendToSelf(IPacket<?> packet) {
        if (this.entry.trackedEntity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) this.entry.trackedEntity;
            QueuingConnection.enqueueSend(player.connection, packet);
        }
    }

    private int getEffectiveTrackDistance() {
        Entity entity = this.entry.trackedEntity;
        if (!entity.isBeingRidden()) {
            return this.adjustTrackDistance(this.maxTrackDistance);
        }

        int maxDistance = this.maxTrackDistance;
        for (Entity passenger : entity.getRecursivePassengers()) {
            maxDistance = Math.max(maxDistance, passenger.getType().defaultTrackingRangeSupplier());
        }

        return this.adjustTrackDistance(maxDistance);
    }

    private int adjustTrackDistance(int initialDistance) {
        return this.entry.world.getServer().func_230512_b_(initialDistance);
    }

    private static long chunkForEntity(Entity entity) {
        if (!entity.addedToChunk) {
            int x = MathHelper.floor(entity.getPosX()) >> 4;
            int z = MathHelper.floor(entity.getPosZ()) >> 4;
            return ChunkPos.asLong(x, z);
        }

        return ChunkPos.asLong(entity.chunkCoordX, entity.chunkCoordZ);
    }
}

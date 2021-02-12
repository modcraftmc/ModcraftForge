package net.gegy1000.tictacs.chunk.tracker;

import net.gegy1000.tictacs.chunk.ChunkAccess;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.ChunkMap;
import net.gegy1000.tictacs.chunk.ChunkMapListener;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.gegy1000.tictacs.chunk.entry.ChunkEntryTrackers;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketManager;

import java.util.Set;


public final class ChunkTracker implements ChunkMapListener {
    public static final int CHUNK_TICKING_DISTANCE = 8;

    private final ChunkController controller;

    private final ChunkPlayerWatchers players;
    private final ChunkEntityTrackers entities;

    private final ChunkTrackWatcher playerTracker;
    private final ChunkTrackWatcher[] trackWatchers;

    private int viewDistance;

    public ChunkTracker(ServerWorld world, ChunkController controller) {
        this.controller = controller;
        this.players = new ChunkPlayerWatchers(world);
        this.entities = new ChunkEntityTrackers(controller);

        this.playerTracker = new ChunkTrackWatcher(3);
        this.playerTracker.setStartTracking(this::startTrackingChunk);
        this.playerTracker.setStopTracking(this::stopTrackingChunk);
        this.playerTracker.setUpdateTracking((player, pos, entry) -> {
            entry.getTrackers().updateTrackingPlayer(player);
        });

        ChunkTrackWatcher tickingTracker = new ChunkTrackWatcher(CHUNK_TICKING_DISTANCE);
        tickingTracker.setStartTracking(this::startTrackingChunkTickable);
        tickingTracker.setStopTracking(this::stopTrackingChunkTickable);

        this.trackWatchers = new ChunkTrackWatcher[] {
                this.playerTracker,
                tickingTracker
        };
    }

    public static int getChunkDistance(ServerPlayerEntity player, int chunkX, int chunkZ) {
        SectionPos playerChunk = player.getManagedSectionPos();
        int deltaX = playerChunk.getX() - chunkX;
        int deltaZ = playerChunk.getZ() - chunkZ;

        return Math.max(Math.abs(deltaX), Math.abs(deltaZ));
    }

    public void setViewDistance(int viewDistance) {
        int lastViewDistance = this.viewDistance;
        this.viewDistance = viewDistance;

        this.playerTracker.setRadius(viewDistance);

        for (ServerPlayerEntity player : this.players) {
            SectionPos chunkPos = player.getManagedSectionPos();
            int chunkX = chunkPos.getX();
            int chunkZ = chunkPos.getZ();

            ChunkTrackView view = ChunkTrackView.withRadius(chunkX, chunkZ, viewDistance);
            ChunkTrackView lastView = ChunkTrackView.withRadius(chunkX, chunkZ, lastViewDistance);

            this.updatePlayerTracker(player, this.playerTracker, view, lastView);
        }
    }

    public void tick() {
        this.entities.tick();

        for (ServerPlayerEntity player : this.players) {
            this.tickPlayer(player);
        }
    }

    private void tickPlayer(ServerPlayerEntity player) {
        SectionPos lastSectionPos = player.getManagedSectionPos();
        SectionPos sectionPos = SectionPos.from(player);

        if (!lastSectionPos.equals(sectionPos)) {
            player.setManagedSectionPos(sectionPos);

            if (lastSectionPos.getX() != sectionPos.getX() || lastSectionPos.getZ() != sectionPos.getZ()) {
                ChunkPackets.sendPlayerChunkPos(player);
            }
        }

        this.updatePlayerTickets(player, lastSectionPos, sectionPos);
        this.updatePlayerTrackers(player, lastSectionPos, sectionPos);
    }

    private void updatePlayerTickets(ServerPlayerEntity player, SectionPos lastSectionPos, SectionPos sectionPos) {
        boolean lastLoadingEnabled = this.players.isLoadingEnabled(player);
        boolean loadingEnabled = this.players.shouldLoadChunks(player);

        TicketManager ticketManager = this.controller.getTicketManager();
        if (lastSectionPos.getX() != sectionPos.getX() || lastSectionPos.getZ() != sectionPos.getZ()) {
            if (lastLoadingEnabled) {
                ticketManager.removePlayer(lastSectionPos, player);
            }
            if (loadingEnabled) {
                ticketManager.updatePlayerPosition(sectionPos, player);
            }
        }

        if (lastLoadingEnabled != loadingEnabled) {
            this.players.setLoadingEnabled(player, loadingEnabled);

            if (loadingEnabled) {
                ticketManager.updatePlayerPosition(sectionPos, player);
            } else {
                ticketManager.removePlayer(lastSectionPos, player);
            }
        }
    }

    private void updatePlayerTrackers(ServerPlayerEntity player, SectionPos lastSectionPos, SectionPos sectionPos) {
        int chunkX = sectionPos.getX();
        int chunkZ = sectionPos.getZ();
        int lastChunkX = lastSectionPos.getX();
        int lastChunkZ = lastSectionPos.getZ();

        if (chunkX == lastChunkX && chunkZ == lastChunkZ) {
            return;
        }

        for (ChunkTrackWatcher tracker : this.trackWatchers) {
            ChunkTrackView view = tracker.viewAt(chunkX, chunkZ);
            ChunkTrackView lastView = tracker.viewAt(lastChunkX, lastChunkZ);

            this.updatePlayerTracker(player, tracker, view, lastView);
        }
    }

    private void updatePlayerTracker(ServerPlayerEntity player, ChunkTrackWatcher tracker, ChunkTrackView view, ChunkTrackView lastView) {
        ChunkAccess chunks = this.controller.getMap().primary();

        ChunkTrackWatcher.Function startTracking = tracker.getStartTracking();
        if (startTracking != null) {
            view.forEachDifference(lastView, pos -> {
                ChunkEntry entry = chunks.getEntry(pos);
                if (entry != null) {
                    startTracking.accept(player, pos, entry);
                }
            });
        }

        ChunkTrackWatcher.Function stopTracking = tracker.getStopTracking();
        if (stopTracking != null) {
            lastView.forEachDifference(view, pos -> {
                ChunkEntry entry = chunks.getEntry(pos);
                if (entry != null) {
                    stopTracking.accept(player, pos, entry);
                }
            });
        }

        ChunkTrackWatcher.Function updateTracking = tracker.getUpdateTracking();
        if (updateTracking != null) {
            view.forEachIntersection(lastView, pos -> {
                ChunkEntry entry = chunks.getEntry(pos);
                if (entry != null) {
                    updateTracking.accept(player, pos, entry);
                }
            });
        }
    }

    private void startTrackingChunk(ServerPlayerEntity player, long pos, ChunkEntry entry) {
        if (entry.getTrackers().addTrackingPlayer(player)) {
            Chunk chunk = entry.getChunkIfComplete();
            if (chunk != null) {
                ChunkPackets.Data dataPackets = ChunkPackets.dataFor(chunk);
                dataPackets.sendTo(player);

                ChunkPackets.Entities entities = ChunkPackets.entitiesFor(entry);
                entities.sendTo(player);
            }
        }
    }

    private void stopTrackingChunk(ServerPlayerEntity player, long pos, ChunkEntry entry) {
        if (entry.getTrackers().removeTrackingPlayer(player)) {
            player.sendChunkUnload(new ChunkPos(pos));
        }
    }

    private void startTrackingChunkTickable(ServerPlayerEntity player, long pos, ChunkEntry entry) {
        ChunkEntryTrackers trackers = entry.getTrackers();

        if (trackers.getTickableTrackingPlayers().isEmpty()) {
            ChunkMap map = this.controller.getMap();
            map.getTickingMaps().addTickableChunk(entry);
        }

        trackers.addTickableTrackingPlayer(player);
    }

    private void stopTrackingChunkTickable(ServerPlayerEntity player, long pos, ChunkEntry entry) {
        ChunkEntryTrackers trackers = entry.getTrackers();
        trackers.removeTickableTrackingPlayer(player);

        if (trackers.getTickableTrackingPlayers().isEmpty()) {
            ChunkMap map = this.controller.getMap();
            map.getTickingMaps().removeTickableChunk(entry);
        }
    }

    public void addPlayer(ServerPlayerEntity player) {
        this.players.addPlayer(player);

        SectionPos sectionPos = SectionPos.from(player);
        player.setManagedSectionPos(sectionPos);
        ChunkPackets.sendPlayerChunkPos(player);

        boolean loadChunks = this.players.shouldLoadChunks(player);
        this.players.setLoadingEnabled(player, loadChunks);
        if (loadChunks) {
            this.controller.getTicketManager().updatePlayerPosition(sectionPos, player);
        }

        ChunkAccess chunks = this.controller.getMap().primary();
        for (ChunkTrackWatcher tracker : this.trackWatchers) {
            ChunkTrackWatcher.Function startTracking = tracker.getStartTracking();
            if (startTracking == null) {
                continue;
            }

            tracker.viewAt(sectionPos).forEach(pos -> {
                ChunkEntry entry = chunks.getEntry(pos);
                if (entry != null) {
                    startTracking.accept(player, pos, entry);
                }
            });
        }
    }

    public void removePlayer(ServerPlayerEntity player) {
        SectionPos sectionPos = player.getManagedSectionPos();

        boolean loadChunks = this.players.isLoadingEnabled(player);
        if (loadChunks) {
            this.controller.getTicketManager().removePlayer(sectionPos, player);
        }

        this.players.removePlayer(player);

        ChunkAccess chunks = this.controller.getMap().primary();
        for (ChunkTrackWatcher tracker : this.trackWatchers) {
            ChunkTrackWatcher.Function stopTracking = tracker.getStopTracking();
            if (stopTracking == null) {
                continue;
            }

            tracker.viewAt(sectionPos).forEach(pos -> {
                ChunkEntry entry = chunks.getEntry(pos);
                if (entry != null) {
                    stopTracking.accept(player, pos, entry);
                }
            });
        }
    }

    @Override
    public void onAddChunk(ChunkEntry entry) {
        ChunkPos chunkPos = entry.getPosition();
        long chunkKey = chunkPos.asLong();

        for (ServerPlayerEntity player : this.players) {
            int distance = getChunkDistance(player, chunkPos.x, chunkPos.z);

            for (ChunkTrackWatcher tracker : this.trackWatchers) {
                if (distance > tracker.getRadius()) {
                    continue;
                }

                ChunkTrackWatcher.Function startTracking = tracker.getStartTracking();
                if (startTracking != null) {
                    startTracking.accept(player, chunkKey, entry);
                }
            }
        }
    }

    @Override
    public void onRemoveChunk(ChunkEntry entry) {
        ChunkPos chunkPos = entry.getPosition();
        long chunkKey = chunkPos.asLong();

        for (ServerPlayerEntity player : this.players) {
            int distance = getChunkDistance(player, chunkPos.x, chunkPos.z);

            for (ChunkTrackWatcher tracker : this.trackWatchers) {
                if (distance > tracker.getRadius()) {
                    continue;
                }

                ChunkTrackWatcher.Function stopTracking = tracker.getStopTracking();
                if (stopTracking != null) {
                    stopTracking.accept(player, chunkKey, entry);
                }
            }
        }
    }

    public void onChunkFull(ChunkEntry entry, Chunk chunk) {
        ChunkPackets.Data data = ChunkPackets.dataFor(chunk);
        ChunkPackets.Entities entities = ChunkPackets.entitiesFor(entry);

        Set<ServerPlayerEntity> trackingPlayers = entry.getTrackers().getTrackingPlayers();
        for (ServerPlayerEntity player : trackingPlayers) {
            data.sendTo(player);
            entities.sendTo(player);
        }
    }

    public ChunkEntityTrackers getEntities() {
        return this.entities;
    }
}

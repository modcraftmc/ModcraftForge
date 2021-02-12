package net.gegy1000.tictacs.chunk;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.gegy1000.tictacs.TicTacs;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.gegy1000.tictacs.config.TicTacsConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.Tuple;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class ChunkLevelTracker {
    public static final int FULL_LEVEL = TicTacsConfig.get().maxViewDistance + 1;
    public static final int MAX_LEVEL = FULL_LEVEL + ChunkStep.getMaxDistance() + 1;

    public static final int LIGHT_TICKET_LEVEL = FULL_LEVEL + ChunkStep.getDistanceFromFull(ChunkStep.GENERATION);

    private final ServerWorld world;
    private final ChunkController controller;

    // Debug only!
    private final Queue<Tuple<Long, Integer>> ticketCache = new ArrayDeque<>();

    public ChunkLevelTracker(ServerWorld world, ChunkController controller) {
        this.world = world;
        this.controller = controller;
    }

    public ChunkEntry setLevel(long pos, int toLevel, ChunkEntry entry, int fromLevel) {
        if (isUnloaded(fromLevel) && isUnloaded(toLevel)) {
            return entry;
        }

        if (TicTacsConfig.get().debug.chunkLevels) {
            this.sendDebugLevel(pos, toLevel);
        }

        if (entry != null) {
            return this.updateLevel(pos, toLevel, entry);
        } else {
            return this.createAtLevel(pos, toLevel);
        }
    }

    private ChunkEntry updateLevel(long pos, int toLevel, ChunkEntry entry) {
        entry.setChunkLevel(toLevel);

        ChunkController accessor = this.controller;
        LongSet unloadedChunks = accessor.asTacs().unloadableChunks;

        if (isUnloaded(toLevel)) {
            unloadedChunks.add(pos);
        } else {
            unloadedChunks.remove(pos);
        }

        return entry;
    }

    private ChunkEntry createAtLevel(long pos, int toLevel) {
        if (isUnloaded(toLevel)) {
            return null;
        }

        return this.controller.getMap().loadEntry(pos, toLevel);
    }

    public static boolean isLoaded(int level) {
        return level <= MAX_LEVEL;
    }

    public static boolean isUnloaded(int level) {
        return level > MAX_LEVEL;
    }

    private void sendDebugLevel(long pos, int toLevel) {
        List<PlayerEntity> players = new ArrayList<>(world.getPlayers());

        if (players.size() > 0) {
            players.forEach(player -> this.sendDebugChunkTicketData(player, pos, toLevel));

            while (this.ticketCache.size() > 0) {
                Tuple<Long, Integer> val = this.ticketCache.poll();
                players.forEach(player -> this.sendDebugChunkTicketData(player, val.getA(), val.getB()));
            }
        } else {
            this.ticketCache.add(new Tuple<>(pos, toLevel));
        }
    }

    private void sendDebugChunkTicketData(PlayerEntity player, long pos, int toLevel) {
        PacketBuffer data = new PacketBuffer(Unpooled.buffer());

        data.writeLong(pos);
        data.writeInt(toLevel);

        ((ServerPlayerEntity) player).connection.sendPacket(new SCustomPayloadPlayPacket(TicTacs.DEBUG_CHUNK_TICKETS, data));
    }
}

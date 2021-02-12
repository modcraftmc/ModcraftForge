package net.gegy1000.tictacs.chunk.tracker;

import net.gegy1000.tictacs.QueuingConnection;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.lighting.WorldLightManager;

import java.util.ArrayList;
import java.util.List;

public final class ChunkPackets {
    public static Data dataFor(Chunk chunk) {
        return new Data(chunk);
    }

    public static Entities entitiesFor(ChunkEntry entry) {
        Entities entities = new Entities();
        for (ChunkEntityTracker tracker : entry.getTrackers().getEntities()) {
            entities.addEntity(tracker.getEntity());
        }

        return entities;
    }

    public static void sendPlayerChunkPos(ServerPlayerEntity player) {
        SectionPos pos = player.getManagedSectionPos();
        QueuingConnection.enqueueSend(player.connection, new SUpdateChunkPositionPacket(pos.getSectionX(), pos.getSectionZ()));
    }

    public static class Data {
        private final Chunk chunk;

        private SChunkDataPacket dataPacket;
        private SUpdateLightPacket lightPacket;

        Data(Chunk chunk) {
            this.chunk = chunk;
        }

        public void sendTo(ServerPlayerEntity player) {
            ChunkPos chunkPos = this.chunk.getPos();

            if (this.dataPacket == null) {
                WorldLightManager lighting = this.chunk.getWorld().getLightManager();

                this.dataPacket = new SChunkDataPacket(this.chunk, 0xFFFF);
                this.lightPacket = new SUpdateLightPacket(chunkPos, lighting, true);
            }

            player.sendChunkLoad(chunkPos, this.dataPacket, this.lightPacket);
        }
    }

    public static class Entities {
        private final List<MobEntity> leashedEntities = new ArrayList<>();
        private final List<Entity> entitiesWithPassengers = new ArrayList<>();

        Entities() {
        }

        public void addEntity(Entity entity) {
            if (entity instanceof MobEntity && ((MobEntity) entity).getLeashHolder() != null) {
                this.leashedEntities.add((MobEntity) entity);
            }

            if (!entity.getPassengers().isEmpty()) {
                this.entitiesWithPassengers.add(entity);
            }
        }

        public void sendTo(ServerPlayerEntity player) {
            for (MobEntity entity : this.leashedEntities) {
                QueuingConnection.enqueueSend(player.connection, new SMountEntityPacket(entity, entity.getLeashHolder()));
            }

            for (Entity entity : this.entitiesWithPassengers) {
                QueuingConnection.enqueueSend(player.connection, new SSetPassengersPacket(entity));
            }
        }
    }
}

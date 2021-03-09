package org.spigotmc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

public class TrackingRange {


    /**
     * Gets the range an entity should be 'tracked' by players and visible in
     * the client.
     *
     * @param entity
     * @param defaultRange Default range defined by Mojang
     * @return
     */
    public static int getEntityTrackingRange(Entity entity, int defaultRange)
    {
        if (entity instanceof EnderDragonEntity) return defaultRange; // Paper - enderdragon is exempt
        SpigotWorldConfig config = entity.world.spigotConfig;
        if ( entity instanceof ServerPlayerEntity)
        {
            return config.playerTrackingRange;
            // Paper start - Simplify and set water mobs to animal tracking range
        }
        switch (entity.activationType) {
            case RAIDER:
            case MONSTER:
            case FLYING_MONSTER:
                return config.monsterTrackingRange;
            case WATER:
            case VILLAGER:
            case ANIMAL:
                return config.animalTrackingRange;
            case MISC:
        }
        if ( entity instanceof ItemFrameEntity || entity instanceof PaintingEntity || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity)
        // Paper end
        {
            return config.miscTrackingRange;
        } else
        {
            return config.otherTrackingRange;
        }
    }

    // Paper start - optimise entity tracking
    // copied from above, TODO check on update
    public static TrackingRangeType getTrackingRangeType(Entity entity)
    {
        if (entity instanceof EnderDragonEntity) return TrackingRangeType.ENDERDRAGON; // Paper - enderdragon is exempt
        if ( entity instanceof ServerPlayerEntity )
        {
            return TrackingRangeType.PLAYER;
            // Paper start - Simplify and set water mobs to animal tracking range
        }
        switch (entity.activationType) {
            case RAIDER:
            case MONSTER:
            case FLYING_MONSTER:
                return TrackingRangeType.MONSTER;
            case WATER:
            case VILLAGER:
            case ANIMAL:
                return TrackingRangeType.ANIMAL;
            case MISC:
        }
        if ( entity instanceof ItemFrameEntity || entity instanceof PaintingEntity || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity )
        // Paper end
        {
            return TrackingRangeType.MISC;
        } else
        {
            return TrackingRangeType.OTHER;
        }
    }

    public static enum TrackingRangeType {
        PLAYER,
        ANIMAL,
        MONSTER,
        MISC,
        OTHER,
        ENDERDRAGON;
    }
    // Paper end - optimise entity tracking
}

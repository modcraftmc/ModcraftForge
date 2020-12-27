package fr.modcraftmc.forge.spigot;
import fr.modcraftmc.forge.config.ModcraftConfig;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPartEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.item.FireworkRocketEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.passive.horse.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Collection;
import java.util.List;

public class ActivationRange
{

    public enum ActivationType
    {
        WATER, // Paper
        MONSTER,
        ANIMAL,
        RAIDER,
        MISC;

        AxisAlignedBB boundingBox = new AxisAlignedBB( 0, 0, 0, 0, 0, 0 );
    }

    static AxisAlignedBB maxBB = new AxisAlignedBB( 0, 0, 0, 0, 0, 0 );

    /**
     * Initializes an entities type on construction to specify what group this
     * entity is in for activation ranges.
     *
     * @param entity
     * @return group id
     */
    public static ActivationType initializeEntityActivationType(Entity entity)
    {
        if (entity instanceof WaterMobEntity) { return ActivationType.WATER; } // Paper
        if ( entity instanceof AbstractRaiderEntity)
        {
            return ActivationType.RAIDER;
        } else if ( entity instanceof MonsterEntity || entity instanceof SlimeEntity)
        {
            return ActivationType.MONSTER;
        } else if ( entity instanceof CreatureEntity || entity instanceof AmbientEntity)
        {
            return ActivationType.ANIMAL;
        } else
        {
            return ActivationType.MISC;
        }
    }

    /**
     * These entities are excluded from Activation range checks.
     *
     * @param entity Entity to initialize
     * @return boolean If it should always tick.
     */
    public static boolean initializeEntityActivationState(Entity entity)
    {
        if ( ( entity.activationType == ActivationType.MISC && ModcraftConfig.getMiscActivationRange() == 0 )
                || ( entity.activationType == ActivationType.RAIDER && ModcraftConfig.getRaiderActivationRange() == 0 )
                || ( entity.activationType == ActivationType.ANIMAL && ModcraftConfig.getAnimalActivationRange() == 0 )
                || ( entity.activationType == ActivationType.MONSTER && ModcraftConfig.getMonsterActivationRange() == 0 )
                || ( entity.activationType == ActivationType.WATER && ModcraftConfig.getWaterActivationRange() == 0 ) // Paper
                || entity instanceof PlayerEntity
                || entity instanceof AbstractArrowEntity
                || entity instanceof EnderDragonEntity
                || entity instanceof EnderDragonPartEntity
                || entity instanceof WitherEntity
                || entity instanceof FireballEntity
                || entity instanceof LightningBoltEntity
                || entity instanceof TNTEntity
                || entity instanceof FallingBlockEntity // Paper - Always tick falling blocks
                || entity instanceof EnderCrystalEntity
                || entity instanceof FireworkRocketEntity
                || entity instanceof TridentEntity)
        {
            return true;
        }

        return false;
    }

    /**
     * Find what entities are in range of the players in the world and set
     * active if in range.
     *
     * @param world
     */
    public static void activateEntities(World world)
    {
        //MinecraftTimings.entityActivationCheckTimer.startTiming();
        final int miscActivationRange = ModcraftConfig.getMiscActivationRange();
        final int raiderActivationRange = ModcraftConfig.getRaiderActivationRange();
        final int animalActivationRange = ModcraftConfig.getAnimalActivationRange();
        final int monsterActivationRange = ModcraftConfig.getMonsterActivationRange();
        final int waterActivationRange = ModcraftConfig.getWaterActivationRange(); // Paper

        int maxRange = Math.max( monsterActivationRange, animalActivationRange );
        maxRange = Math.max( maxRange, raiderActivationRange );
        maxRange = Math.max( maxRange, miscActivationRange );
        maxRange = Math.min( ( ModcraftConfig.getViewDistance() << 4 ) - 8, maxRange );

        for ( PlayerEntity player : world.getPlayers() )
        {

            player.activatedTick = MinecraftServer.currentTick;
            maxBB = player.getBoundingBox().grow( maxRange, 256, maxRange );
            ActivationType.MISC.boundingBox = player.getBoundingBox().grow( miscActivationRange, 256, miscActivationRange );
            ActivationType.RAIDER.boundingBox = player.getBoundingBox().grow( raiderActivationRange, 256, raiderActivationRange );
            ActivationType.ANIMAL.boundingBox = player.getBoundingBox().grow( animalActivationRange, 256, animalActivationRange );
            ActivationType.MONSTER.boundingBox = player.getBoundingBox().grow( monsterActivationRange, 256, monsterActivationRange );
            ActivationType.WATER.boundingBox = player.getBoundingBox().grow( waterActivationRange, 256, waterActivationRange ); // Paper


            int i = MathHelper.floor( maxBB.minX / 16.0D );
            int j = MathHelper.floor( maxBB.maxX / 16.0D );
            int k = MathHelper.floor( maxBB.minZ / 16.0D );
            int l = MathHelper.floor( maxBB.maxZ / 16.0D );

            for ( int i1 = i; i1 <= j; ++i1 )
            {
                for ( int j1 = k; j1 <= l; ++j1 )
                {
                    Chunk chunk = (Chunk) world.getChunk( i1, j1 ); //TODO IMPLEMENT PAPER CHUNK PROVIDER getChunkIfLoadedImmediately
                    if ( chunk != null )
                    {
                        activateChunkEntities( chunk );
                    }
                }
            }
        }
        //MinecraftTimings.entityActivationCheckTimer.stopTiming();
    }

    /**
     * Checks for the activation state of all entities in this chunk.
     *
     * @param chunk
     */
    private static void activateChunkEntities(Chunk chunk)
    {
        for ( List<Entity> slice : chunk.entitySlices )
        {
            for ( Entity entity : (Collection<Entity>) slice )
            {
                if ( MinecraftServer.currentTick > entity.activatedTick )
                {
                    if ( entity.defaultActivationState )
                    {
                        entity.activatedTick = MinecraftServer.currentTick;
                        continue;
                    }
                    if ( entity.activationType.boundingBox.intersects( entity.getBoundingBox() ) )
                    {
                        entity.activatedTick = MinecraftServer.currentTick;
                    }
                }
            }
        }
    }

    /**
     * If an entity is not in range, do some more checks to see if we should
     * give it a shot.
     *
     * @param entity
     * @return
     */
    public static boolean checkEntityImmunities(Entity entity)
    {
        // quick checks.
        if ( entity.isInWater() || entity.fire > 0 )
        {
            return true;
        }
        if ( !( entity instanceof ArrowEntity) )
        {
            if ( !entity.onGround || !entity.getPassengers().isEmpty() || entity.isPassenger() )
            {
                return true;
            }
        } else if ( !( (ArrowEntity) entity ).onGround )
        {
            return true;
        }
        // special cases.
        if ( entity instanceof LivingEntity)
        {
            LivingEntity living = (LivingEntity) entity;
            if ( /*TODO: Missed mapping? living.attackTicks > 0 || */ living.hurtResistantTime > 0 || living.getActivePotionEffects().size() > 0 )
            {
                return true;
            }
            if ( entity instanceof CreatureEntity && (( (CreatureEntity) entity ).getAttackTarget() != null || ( (CreatureEntity) entity ).getMovingTarget() != null)) // Paper
            {
                return true;
            }
            if ( entity instanceof VillagerEntity && ( (VillagerEntity) entity ).canBreed() )
            {
                return true;
            }
            // Paper start
            if ( entity instanceof LlamaEntity && ( (LlamaEntity ) entity ).inCaravan() )
            {
                return true;
            }
            // Paper end
            if ( entity instanceof AnimalEntity)
            {
                AnimalEntity animal = (AnimalEntity) entity;
                if ( animal.isChild() || animal.isInLove() )
                {
                    return true;
                }
                if ( entity instanceof SheepEntity && ( (SheepEntity) entity ).getSheared() )
                {
                    return true;
                }
            }
            if (entity instanceof CreeperEntity && ((CreeperEntity) entity).hasIgnited()) { // isExplosive
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the entity is active for this tick.
     *
     * @param entity
     * @return
     */
    public static boolean checkIfActive(Entity entity)
    {
        // Never safe to skip fireworks or entities not yet added to chunk
        if ( !entity.addedToChunk || entity instanceof FireworkRocketEntity ) {
            return true;
        }

        boolean isActive = entity.activatedTick >= MinecraftServer.currentTick || entity.defaultActivationState;

        // Should this entity tick?
        if ( !isActive )
        {
            if ( ( MinecraftServer.currentTick - entity.activatedTick - 1 ) % 20 == 0 )
            {
                // Check immunities every 20 ticks.
                if ( checkEntityImmunities( entity ) )
                {
                    // Triggered some sort of immunity, give 20 full ticks before we check again.
                    entity.activatedTick = MinecraftServer.currentTick + 20;
                }
                isActive = true;
                // Paper start
            } else if (entity instanceof MobEntity && ((MobEntity) entity).targetSelector.hasTasks()) {
                isActive = true;
            }
            // Paper end
            // Add a little performance juice to active entities. Skip 1/4 if not immune.
        } else if ( !entity.defaultActivationState && entity.ticksExisted % 4 == 0 && !(entity instanceof MobEntity && ((MobEntity) entity).targetSelector.hasTasks()) && !checkEntityImmunities( entity ) ) // Paper - add targetSelector.hasTasks
        {
            isActive = false;
        }
        return isActive;
    }
}

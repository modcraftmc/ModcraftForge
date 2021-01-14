package fr.modcraftmc.forge.paper;

import fr.modcraftmc.forge.config.PaperConfig;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.schedule.Activity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPartEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.passive.horse.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

public class ActivationRange {

    public enum ActivationType
    {
        WATER, // Paper
        FLYING_MONSTER, // Paper
        VILLAGER, // Paper
        MONSTER,
        ANIMAL,
        RAIDER,
        MISC;

        AxisAlignedBB boundingBox = new AxisAlignedBB( 0, 0, 0, 0, 0, 0 );
    }
    // Paper start

    static Activity[] VILLAGER_PANIC_IMMUNITIES = {
            Activity.HIDE,
            Activity.PRE_RAID,
            Activity.RAID,
            Activity.PANIC
    };

    private static int checkInactiveWakeup(Entity entity) {
        World world = entity.world;
        PaperConfig config = PaperConfig.GetConfigs();
        long inactiveFor = MinecraftServer.currentTick - entity.activatedTick;
        if (entity.activationType == ActivationType.VILLAGER) {
            if (inactiveFor > config.wakeUpInactiveVillagersEvery && world.wakeupInactiveRemainingVillagers > 0) {
                world.wakeupInactiveRemainingVillagers--;
                return config.wakeUpInactiveVillagersFor;
            }
        } else if (entity.activationType == ActivationType.ANIMAL) {
            if (inactiveFor > config.wakeUpInactiveAnimalsEvery && world.wakeupInactiveRemainingAnimals > 0) {
                world.wakeupInactiveRemainingAnimals--;
                return config.wakeUpInactiveAnimalsFor;
            }
        } else if (entity.activationType == ActivationType.FLYING_MONSTER) {
            if (inactiveFor > config.wakeUpInactiveFlyingEvery && world.wakeupInactiveRemainingFlying > 0) {
                world.wakeupInactiveRemainingFlying--;
                return config.wakeUpInactiveFlyingFor;
            }
        } else if (entity.activationType == ActivationType.MONSTER || entity.activationType == ActivationType.RAIDER) {
            if (inactiveFor > config.wakeUpInactiveMonstersEvery && world.wakeupInactiveRemainingMonsters > 0) {
                world.wakeupInactiveRemainingMonsters--;
                return config.wakeUpInactiveMonstersFor;
            }
        }
        return -1;
    }
    // Paper end

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
        else if (entity instanceof VillagerEntity) { return ActivationType.VILLAGER; } // Paper
        else if (entity instanceof FlyingEntity && entity instanceof IMob) { return ActivationType.FLYING_MONSTER; } // Paper - doing & Monster incase Flying no longer includes monster in future
        if ( entity instanceof AbstractRaiderEntity)
        {
            return ActivationType.RAIDER;
        } else if ( entity instanceof IMob ) // Paper - correct monster check
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
        PaperConfig config = PaperConfig.GetConfigs();
        if ( ( entity.activationType == ActivationType.MISC && config.miscActivationRange <= 0 )
                || ( entity.activationType == ActivationType.RAIDER && config.raiderActivationRange <= 0 )
                || ( entity.activationType == ActivationType.ANIMAL && config.animalActivationRange <= 0 )
                || ( entity.activationType == ActivationType.MONSTER && config.monsterActivationRange <= 0 )
                || ( entity.activationType == ActivationType.VILLAGER && config.villagerActivationRange <= 0 ) // Paper
                || ( entity.activationType == ActivationType.WATER && config.waterActivationRange <= 0 ) // Paper
                || ( entity.activationType == ActivationType.FLYING_MONSTER && config.flyingMonsterActivationRange <= 0 ) // Paper
                || entity instanceof EnderCrystalEntity // Paper
                || entity instanceof PlayerEntity
                || entity instanceof ProjectileEntity
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
        final int miscActivationRange = world.spigotConfig.miscActivationRange;
        final int raiderActivationRange = world.spigotConfig.raiderActivationRange;
        final int animalActivationRange = world.spigotConfig.animalActivationRange;
        final int monsterActivationRange = world.spigotConfig.monsterActivationRange;
        // Paper start
        final int waterActivationRange = world.spigotConfig.waterActivationRange;
        final int flyingActivationRange = world.spigotConfig.flyingMonsterActivationRange;
        final int villagerActivationRange = world.spigotConfig.villagerActivationRange;
        world.wakeupInactiveRemainingAnimals = Math.min(world.wakeupInactiveRemainingAnimals + 1, world.spigotConfig.wakeUpInactiveAnimals);
        world.wakeupInactiveRemainingVillagers = Math.min(world.wakeupInactiveRemainingVillagers + 1, world.spigotConfig.wakeUpInactiveVillagers);
        world.wakeupInactiveRemainingMonsters = Math.min(world.wakeupInactiveRemainingMonsters + 1, world.spigotConfig.wakeUpInactiveMonsters);
        world.wakeupInactiveRemainingFlying = Math.min(world.wakeupInactiveRemainingFlying + 1, world.spigotConfig.wakeUpInactiveFlying);
        final ServerChunkProvider chunkProvider = (ServerChunkProvider) world.getChunkProvider();
        // Paper end

        int maxRange = Math.max( monsterActivationRange, animalActivationRange );
        maxRange = Math.max( maxRange, raiderActivationRange );
        maxRange = Math.max( maxRange, miscActivationRange );
        // Paper start
        maxRange = Math.max( maxRange, flyingActivationRange );
        maxRange = Math.max( maxRange, waterActivationRange );
        maxRange = Math.max( maxRange, villagerActivationRange );
        // Paper end
        maxRange = Math.min( ( ((ServerWorld)world).getChunkProvider().chunkManager.getEffectiveViewDistance() << 4 ) - 8, maxRange ); // Paper - no-tick view distance

        for ( PlayerEntity player : world.getPlayers() )
        {

            player.activatedTick = MinecraftServer.currentTick;
            maxBB = player.getBoundingBox().grow( maxRange, 256, maxRange );
            ActivationType.MISC.boundingBox = player.getBoundingBox().grow( miscActivationRange, 256, miscActivationRange );
            ActivationType.RAIDER.boundingBox = player.getBoundingBox().grow( raiderActivationRange, 256, raiderActivationRange );
            ActivationType.ANIMAL.boundingBox = player.getBoundingBox().grow( animalActivationRange, 256, animalActivationRange );
            ActivationType.MONSTER.boundingBox = player.getBoundingBox().grow( monsterActivationRange, 256, monsterActivationRange );
            // Paper start
            ActivationType.WATER.boundingBox = player.getBoundingBox().grow( waterActivationRange, 256, waterActivationRange );
            ActivationType.FLYING_MONSTER.boundingBox = player.getBoundingBox().grow( flyingActivationRange, 256, flyingActivationRange );
            ActivationType.VILLAGER.boundingBox = player.getBoundingBox().grow( villagerActivationRange, 256, waterActivationRange );
            // Paper end

            int i = MathHelper.floor( maxBB.minX / 16.0D );
            int j = MathHelper.floor( maxBB.maxX / 16.0D );
            int k = MathHelper.floor( maxBB.minZ / 16.0D );
            int l = MathHelper.floor( maxBB.maxZ / 16.0D );

            for ( int i1 = i; i1 <= j; ++i1 )
            {
                for ( int j1 = k; j1 <= l; ++j1 )
                {
                    Chunk chunk = chunkProvider.getChunkNow( i1, j1 ); // Paper
                    if ( chunk != null )
                    {
                        activateChunkEntities( chunk );
                    }
                }
            }
        }
    }

    /**
     * Checks for the activation state of all entities in this chunk.
     *
     * @param chunk
     */
    private static void activateChunkEntities(Chunk chunk)
    {
        // Paper start
        Entity[] rawData = chunk.entities.getRawData();
        for (int i = 0; i < chunk.entities.size(); i++) {
            Entity entity = rawData[i];
            //for ( Entity entity : (Collection<Entity>) slice )
            // Paper end
            {
                if (MinecraftServer.currentTick > entity.activatedTick) {
                    if (entity.defaultActivationState || entity.activationType.boundingBox.intersects(entity.getBoundingBox())) { // Paper
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
    public static int checkEntityImmunities(Entity entity) // Paper - return # of ticks to get immunity
    {
        // Paper start
        PaperConfig config = entity.world.spigotConfig;
        int inactiveWakeUpImmunity = checkInactiveWakeup(entity);
        if (inactiveWakeUpImmunity > -1) {
            return inactiveWakeUpImmunity;
        }
        if (entity.fire > 0) {
            return 2;
        }
        long inactiveFor = MinecraftServer.currentTick - entity.activatedTick;
        // Paper end
        // quick checks.
        if ( (entity.activationType != ActivationType.WATER && entity.isInWater() && entity.isPushedByWater()) ) // Paper
        {
            return 100; // Paper
        }
        if ( !( entity instanceof ArrowEntity) )
        {
            if ( (!entity.isOnGround() && !(entity instanceof FlyingEntity)) ) // Paper - remove passengers logic
            {
                return 10; // Paper
            }
        } else if ( !( (ArrowEntity) entity ).isOnGround() )
        {
            return 1; // Paper
        }
        // special cases.
        if ( entity instanceof LivingEntity)
        {
            LivingEntity living = (LivingEntity) entity;
            if ( living.isOnLadder() || living.isJumping || living.hurtTime > 0 || living.activePotionsMap.size() > 0 ) // Paper
            {
                return 1; // Paper
            }
            if ( entity instanceof MobEntity && ((MobEntity) entity ).getAttackTarget() != null) // Paper
            {
                return 20; // Paper
            }
            // Paper start
            if (entity instanceof BeeEntity) {
                BeeEntity bee = (BeeEntity)entity;
                BlockPos movingTarget = bee.getMovingTarget();
                if (bee.func_233678_J__() ||
                        (bee.getHivePos() != null && bee.getHivePos().equals(movingTarget)) ||
                        (bee.getFlowerPos() != null && bee.getFlowerPos().equals(movingTarget))
                ) {
                    return 20;
                }
            }
            if ( entity instanceof VillagerEntity ) {
                Brain<VillagerEntity> behaviorController = ((VillagerEntity) entity).getBrain();

                if (config.villagersActiveForPanic) {
                    for (Activity activity : VILLAGER_PANIC_IMMUNITIES) {
                        if (behaviorController.hasActivity(activity)) {
                            return 20*5;
                        }
                    }
                }

                if (config.villagersWorkImmunityAfter > 0 && inactiveFor >= config.villagersWorkImmunityAfter) {
                    if (behaviorController.hasActivity(Activity.WORK)) {
                        return config.villagersWorkImmunityFor;
                    }
                }
            }
            if ( entity instanceof LlamaEntity && ( (LlamaEntity ) entity ).inCaravan() )
            {
                return 1;
            }
            // Paper end
            if ( entity instanceof AnimalEntity)
            {
                AnimalEntity animal = (AnimalEntity) entity;
                if ( animal.isChild() || animal.isInLove() )
                {
                    return 5; // Paper
                }
                if ( entity instanceof SheepEntity && ( (SheepEntity) entity ).getSheared() )
                {
                    return 1; // Paper
                }
            }
            if (entity instanceof CreeperEntity && ((CreeperEntity) entity).hasIgnited()) { // isExplosive
                return 20; // Paper
            }
            // Paper start
            if (entity instanceof MobEntity && ((MobEntity) entity).targetSelector.hasTasks() ) {
                return 0;
            }
            if (entity instanceof PillagerEntity) {
                PillagerEntity pillager = (PillagerEntity) entity;
                // TODO:?
            }
            // Paper end
        }
        return -1; // Paper
    }

    /**
     * Checks if the entity is active for this tick.
     *
     * @param entity
     * @return
     */
    public static boolean checkIfActive(Entity entity)
    {
        if (entity instanceof SquidEntity) return true; // Purpur
        // Never safe to skip fireworks or entities not yet added to chunk
        if ( !entity.addedToChunk || entity instanceof FireworkRocketEntity ) {
            return true;
        }
        // Paper start - special case always immunities
        // immunize brand new entities, dead entities, and portal scenarios
        if (entity.defaultActivationState || entity.ticksExisted < 20*10 || !entity.isAlive() || entity.inPortal || entity.portalCounter > 0) {
            return true;
        }
        // immunize leashed entities
        if (entity instanceof MobEntity && ((MobEntity)entity).leashHolder instanceof PlayerEntity) {
            return true;
        }
        // Paper end

        boolean isActive = entity.activatedTick >= MinecraftServer.currentTick;
        entity.isTemporarilyActive = false; // Paper

        // Should this entity tick?
        if ( !isActive )
        {
            if ( ( MinecraftServer.currentTick - entity.activatedTick - 1 ) % 20 == 0 )
            {
                // Check immunities every 20 ticks.
                // Paper start
                int immunity = checkEntityImmunities(entity);
                if (immunity >= 0) {
                    entity.activatedTick = MinecraftServer.currentTick + immunity;
                } else {
                    entity.isTemporarilyActive = true;
                }
                // Paper end
                isActive = true;

            }
            // Add a little performance juice to active entities. Skip 1/4 if not immune.
        } else if (entity.ticksExisted % 4 == 0 && checkEntityImmunities( entity) < 0 ) // Paper
        {
            isActive = false;
        }
        return isActive;
    }
}

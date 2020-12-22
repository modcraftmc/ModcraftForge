/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2018-2019 DaPorkchop_ and contributors
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package fr.modcraftmc.forge.spigot;

import fr.modcraftmc.forge.config.ModcraftConfig;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.FireworkRocketEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.server.ServerLifecycleHooks;


public class ActivationRange {

    static AxisAlignedBB maxBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB miscBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB animalBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB monsterBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

    /**
     * Initializes an entities type on construction to specify what group this
     * entity is in for activation ranges.
     *
     * @param entity
     * @return group id
     */
    public static byte initializeEntityActivationType(Entity entity) {
        if (entity instanceof IMob) {
            return 1; // Monster
        } else if (entity instanceof CreatureEntity || entity instanceof AmbientEntity) {
            return 2; // Animal
        } else {
            return 3; // Misc
        }
    }

    /**
     * These entities are excluded from Activation range checks.
     *
     * @param entity
     * @return boolean If it should always tick.
     */
    public static boolean initializeEntityActivationState(Entity entity) {
        byte activationType = ((IActivationEntity) entity).getActivationType();

        return (activationType == 3 && ModcraftConfig.getMiscActivationRange() == 0)
                || (activationType == 2 && ModcraftConfig.getAnimalActivationRange() == 0)
                || (activationType == 1 && ModcraftConfig.getMonsterActivationRange() == 0)
                || entity instanceof PlayerEntity
                || entity instanceof IProjectile
                || entity instanceof EnderDragonEntity
                || entity instanceof EnderCrystalEntity
                || entity instanceof WitherEntity
                || entity instanceof FireballEntity
                || entity instanceof LightningBoltEntity
                || entity instanceof TNTEntity
                || entity instanceof FireworkRocketEntity;

    }

    /**
     * Find what entities are in range of the players in the world and set
     * active if in range.
     *
     * @param world
     */
    public static void activateEntities(World world) {
        //SpigotTimings.entityActivationCheckTimer.startTiming();
        final int miscActivationRange = ModcraftConfig.getMiscActivationRange();
        final int animalActivationRange = ModcraftConfig.getAnimalActivationRange();
        final int monsterActivationRange = ModcraftConfig.getMonsterActivationRange();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        int maxRange = Math.max(monsterActivationRange, animalActivationRange);
        maxRange = Math.max(maxRange, miscActivationRange);
        maxRange = Math.min((server.getPlayerList().getViewDistance() << 4) - 8, maxRange);

        for (PlayerEntity player : world.getPlayers()) {

            ((IActivationEntity) player).setActivatedTick(server.getTickCounter());
            maxBB = player.getBoundingBox().grow(maxRange, 256, maxRange);
            miscBB = player.getBoundingBox().grow(miscActivationRange, 256, miscActivationRange);
            animalBB = player.getBoundingBox().grow(animalActivationRange, 256, animalActivationRange);
            monsterBB = player.getBoundingBox().grow(monsterActivationRange, 256, monsterActivationRange);

            int minX = MathHelper.floor(maxBB.minX / 16.0D);
            int maxX = MathHelper.floor(maxBB.maxX / 16.0D);
            int minY = MathHelper.floor(maxBB.minY / 16.0D);
            int maxY = MathHelper.floor(maxBB.maxY / 16.0D);
            int minZ = MathHelper.floor(maxBB.minZ / 16.0D);
            int maxZ = MathHelper.floor(maxBB.maxZ / 16.0D);

            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        if (world.isBlockLoaded(new BlockPos(x * 16, y * 16, z * 16))) {
                            activateChunkEntities(world.getChunk(x, z));
                        }
                    }
                }
            }
        }
        //SpigotTimings.entityActivationCheckTimer.stopTiming();
    }

    /**
     * Checks for the activation state of all entities in this chunk.
     *
     * @param chunk
     */
    private static void activateChunkEntities(Chunk chunk) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        for (ClassInheritanceMultiMap<Entity> slice : chunk.getEntityLists()) {
            for (Entity entity : slice) {
                IActivationEntity activationEntity = (IActivationEntity) entity;
                if (server.getTickCounter() > activationEntity.getActivatedTick()) {
                    if (activationEntity.defaultActivationState()) {
                        activationEntity.setActivatedTick(server.getTickCounter());
                        continue;
                    }
                    switch (activationEntity.getActivationType()) {
                        case 1:
                            if (monsterBB.intersects(entity.getBoundingBox())) {
                                activationEntity.setActivatedTick(server.getTickCounter());
                            }
                            break;
                        case 2:
                            if (animalBB.intersects(entity.getBoundingBox())) {
                                activationEntity.setActivatedTick(server.getTickCounter());
                            }
                            break;
                        case 3:
                        default:
                            if (miscBB.intersects(entity.getBoundingBox())) {
                                activationEntity.setActivatedTick(server.getTickCounter());
                            }
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
    public static boolean checkEntityImmunities(Entity entity) {
        // quick checks.
        if (entity.isInWater() || entity.fire > 0) {
            return true;
        }
        if (!(entity instanceof ArrowEntity)) {
            if (!entity.onGround || !entity.getPassengers().isEmpty()) {
                return true;
            }
        } else if (!((ArrowEntity) entity).onGround) {
            return true;
        }
        // special cases.
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            if ( /*TODO: Missed mapping? living.attackTicks > 0 || */ living.getLastAttackedEntityTime() > 0 ||living.hurtTime > 0 || !living.getActivePotionEffects().isEmpty()) {
                return true;
            }
            if (entity instanceof CreatureEntity && ((CreatureEntity) entity).getAttackTarget() != null) {
                return true;
            }

            /*
            if (entity instanceof VillagerEntity && ((VillagerEntity) entity).isDescending()) {
                return true;
            }

             */


            if (entity instanceof AnimalEntity) {
                AnimalEntity animal = (AnimalEntity) entity;
                if (animal.isChild() || animal.isInLove()) {
                    return true;
                }
                if (entity instanceof SheepEntity && ((SheepEntity) entity).getSheared()) {
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
    public static boolean checkIfActive(Entity entity) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        //SpigotTimings.checkIfActiveTimer.startTiming();
        // Never safe to skip fireworks or entities not yet added to chunk
        // PAIL: inChunk - boolean under datawatchers
        if (!entity.addedToChunk || entity instanceof FireworkRocketEntity) {
            //SpigotTimings.checkIfActiveTimer.stopTiming();
            return true;
        }

        IActivationEntity activationEntity = (IActivationEntity) entity;
        boolean isActive = activationEntity.getActivatedTick() >= server.getTickCounter() || activationEntity.defaultActivationState();

        // Should this entity tick?
        if (!isActive) {
            if ((server.getTickCounter() - activationEntity.getActivatedTick() - 1) % 20 == 0) {
                // Check immunities every 20 ticks.
                if (checkEntityImmunities(entity)) {
                    // Triggered some sort of immunity, give 20 full ticks before we check again.
                    activationEntity.setActivatedTick(server.getTickCounter() + 20);
                }
                isActive = true;
            }
            // Add a little performance juice to active entities. Skip 1/4 if not immune.
        } else if (!activationEntity.defaultActivationState() && entity.ticksExisted % 4 == 0 && !checkEntityImmunities(entity)) {
            isActive = false;
        }
        int x = MathHelper.floor(entity.getPosX());
        int y = MathHelper.floor(entity.getPosY());
        int z = MathHelper.floor(entity.getPosZ());
        // Make sure not on edge of unloaded chunk
        Chunk chunk = entity.world.getChunkProvider().getChunk(x >> 4, z >> 4, true);
        if (isActive && !(chunk != null && entity.world.isAreaLoaded(new BlockPos(((x >> 4) - 1) * 16, ((y >> 4) - 1) * 16, ((z >> 4) - 1) * 16),
                new BlockPos(((x >> 4) + 1) * 16, ((y >> 4) + 1) * 16, ((z >> 4) + 1) * 16)))) {
            isActive = false;
        }
        //SpigotTimings.checkIfActiveTimer.stopTiming();
        return isActive;
    }
}
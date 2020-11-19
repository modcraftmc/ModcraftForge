package fr.modcraftmc.modcraftforge.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;

public class UntrackerTask implements Runnable {

    private static boolean running = false;


    @Override
    public void run() {

        if(ServerLifecycleHooks.getCurrentServer().tickTimeArray[0] > 18) {
            return;
        }
        running = true;

        for (ServerWorld world : ServerLifecycleHooks.getCurrentServer().getWorlds()) {
            untrack(world.getDimension().getType());
        }


        running = false;

    }

    public static void untrack(DimensionType type) {


        Set<Integer> toRemove = new HashSet<>();
        int removed = 0;
        ServerWorld serverWorld = ServerLifecycleHooks.getCurrentServer().getWorld(type);
        ServerChunkProvider cps = serverWorld.getChunkProvider();

        try {
            for (ChunkManager.EntityTracker et : cps.chunkManager.entities.values()) {
                Entity entity = et.entity;

                if(entity instanceof PlayerEntity || entity instanceof EnderDragonEntity) {
                    continue;
                }
                if(entity instanceof ArmorStandEntity && entity.hasCustomName()) {
                    continue;
                }

                boolean remove = false;
                if(et.trackingPlayers.size() == 0) {
                    remove = true;
                }
                else if(et.trackingPlayers.size() == 1) {
                    for(PlayerEntity ep : et.trackingPlayers) {
                        if(!ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().contains(ep)) {
                            remove = true;
                        }
                    }
                    if(!remove) {
                        continue;
                    }
                }
                System.out.println("Test: " + entity.toString());
                if(remove) {
                    System.out.println("untracked: " + entity.toString());
                    toRemove.add(entity.getEntityId());
                    removed++;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(int id : toRemove) {
            cps.chunkManager.entities.remove(id);
        }

    }

    public static boolean isRunning() {
        return running;
    }
}

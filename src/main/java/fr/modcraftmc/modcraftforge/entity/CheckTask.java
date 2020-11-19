package fr.modcraftmc.modcraftforge.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;

public class CheckTask implements Runnable {


    @Override
    public void run() {

        if (UntrackerTask.isRunning()) return;



        for (ServerWorld world : ServerLifecycleHooks.getCurrentServer().getWorlds()) {
            checkWorld(world.getDimension().getType());
        }


    }

    private void checkWorld(DimensionType type) {

        ServerWorld serverWorld = ServerLifecycleHooks.getCurrentServer().getWorld(type);
        ServerChunkProvider cps = serverWorld.getChunkProvider();

        Set<Entity> trackAgain = new HashSet<>();

        for (ServerPlayerEntity player : serverWorld.getPlayers()) {

            for (Entity entity : serverWorld.getEntityRange(player)) {
                if (cps.chunkManager.entities.containsKey(entity.getEntityId()) || !entity.isAlive()) {
                    continue;
                }

                trackAgain.add(entity);
                System.out.println(entity.toString());
            }

            for (Entity entity : trackAgain) {
                cps.chunkManager.track(entity);
            }
        }

    }
}

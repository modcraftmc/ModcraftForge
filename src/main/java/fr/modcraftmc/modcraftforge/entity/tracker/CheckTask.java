package fr.modcraftmc.modcraftforge.entity.tracker;

import fr.modcraftforge.ModcraftForge;
import fr.modcraftmc.modcraftforge.configuration.EntitiesConfiguration;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.HashSet;
import java.util.Set;

public class CheckTask implements Runnable {

    private Marker LOG_CHECK = MarkerManager.getMarker("CHECKER");


    @Override
    public void run() {

        if (UntrackerTask.isRunning()) return;

        if (! EntitiesConfiguration.isUseEntityChecker()) return;

        for (ServerWorld world : ServerLifecycleHooks.getCurrentServer().getWorlds()) {
            checkWorld(world.getDimension().getType());
        }

    }

    private void checkWorld(DimensionType type) {

        ServerWorld serverWorld = ServerLifecycleHooks.getCurrentServer().getWorld(type);
        ServerChunkProvider cps = serverWorld.getChunkProvider();

        Set<Entity> trackAgain = new HashSet<>();

        for (ServerPlayerEntity player : serverWorld.getPlayers()) {

            serverWorld.getParallelEntitiesInRange(player, player.getServerWorld(), 70)
                    .forEach(entity -> {
                        if (!cps.chunkManager.entities.containsKey(entity.getEntityId()) &&  entity.isAlive()) {
                            trackAgain.add(entity);
                            ModcraftForge.LOGGER.info(LOG_CHECK, entity.toString());
                        }
                    });

            for (Entity entity : trackAgain) {
                cps.chunkManager.track(entity);
            }
        }

    }
}

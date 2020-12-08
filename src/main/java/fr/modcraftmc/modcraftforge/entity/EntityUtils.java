package fr.modcraftmc.modcraftforge.entity;

import fr.modcraftmc.modcraftforge.configuration.EntitiesConfiguration;
import fr.modcraftmc.modcraftforge.entity.cache.EntityCache;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class EntityUtils {

    public static Map<Class<? extends Entity>, EntityCache> entityCache = new HashMap<>();

    public static String sanitizeClassName(Class clazz)
    {
        String name = clazz.getName().replace(".", "-");

        return name.replaceAll("[^A-Za-z0-9\\-]", "");
    }

    public static String sanitizeClassName(net.minecraft.entity.Entity entity)
    {
        return sanitizeClassName(entity.getClass());
    }

    public static boolean canEntityTick(net.minecraft.entity.Entity entity, World world)
    {
        if (entity == null) return false;

        if (EntitiesConfiguration.isSkipEntities())
        {
            EntityCache eCache = entityCache.get(entity.getClass());
            if (eCache == null)
            {
                String eConfigPath = sanitizeClassName(entity);
                eCache = new EntityCache(entity.getClass(), world.getWorldInfo().getWorldName().toLowerCase(), eConfigPath, EntitiesConfiguration.getTickinterval());
                entityCache.put(entity.getClass(), eCache);
            }

            if (!(entity instanceof MobEntity)) {
                return true;
            }

            /*
            boolean playerInRange = world.getEntitiesInRange(entity, (ServerWorld) world, 80)
                    .anyMatch(e -> e instanceof PlayerEntity);

            if (!playerInRange) {
                return false;
            }

             */
            //ModcraftForge.LOGGER.info("Player in range of {} : {}", entity.getClass(), playerInRange);

            // Skip tick interval
            return eCache.tickInterval > 0 && (world.getWorldInfo().getGameTime() % eCache.tickInterval == 0L);
        }

        return true;
    }
}

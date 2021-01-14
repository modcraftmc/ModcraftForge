package fr.modcraftmc.forge.tuinity;


import fr.modcraftmc.forge.paper.UnsafeList;
import fr.modcraftmc.forge.utils.ModcraftUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class CachedLists {

    static final UnsafeList<AxisAlignedBB> TEMP_COLLISION_LIST = new UnsafeList<>(1024);
    static boolean tempCollisionListInUse;

    public static UnsafeList<AxisAlignedBB> getTempCollisionList() {
        if (!ModcraftUtils.isPrimaryThread() || tempCollisionListInUse) {
            return new UnsafeList<>(16);
        }
        tempCollisionListInUse = true;
        return TEMP_COLLISION_LIST;
    }

    public static void returnTempCollisionList(List<AxisAlignedBB> list) {
        if (list != TEMP_COLLISION_LIST) {
            return;
        }
        ((UnsafeList)list).setSize(0);
        tempCollisionListInUse = false;
    }

    static final UnsafeList<Entity> TEMP_GET_ENTITIES_LIST = new UnsafeList<>(1024);
    static boolean tempGetEntitiesListInUse;

    public static UnsafeList<Entity> getTempGetEntitiesList() {
        if (!ModcraftUtils.isPrimaryThread() || tempGetEntitiesListInUse) {
            return new UnsafeList<>(16);
        }
        tempGetEntitiesListInUse = true;
        return TEMP_GET_ENTITIES_LIST;
    }

    public static void returnTempGetEntitiesList(List<Entity> list) {
        if (list != TEMP_GET_ENTITIES_LIST) {
            return;
        }
        ((UnsafeList)list).setSize(0);
        tempGetEntitiesListInUse = false;
    }

    static final UnsafeList<Chunk> TEMP_GET_CHUNKS_LIST = new UnsafeList<>(1024);
    static boolean tempGetChunksListInUse;

    public static UnsafeList<Chunk> getTempGetChunksList() {
        if (!ModcraftUtils.isPrimaryThread() || tempGetChunksListInUse) {
            return new UnsafeList<>();
        }
        tempGetChunksListInUse = true;
        return TEMP_GET_CHUNKS_LIST;
    }

    public static void returnTempGetChunksList(List<Chunk> list) {
        if (list != TEMP_GET_CHUNKS_LIST) {
            return;
        }
        ((UnsafeList)list).setSize(0);
        tempGetChunksListInUse = false;
    }

    public static void reset() {
        TEMP_COLLISION_LIST.completeReset();
        TEMP_GET_ENTITIES_LIST.completeReset();
        TEMP_GET_CHUNKS_LIST.completeReset();
    }
}

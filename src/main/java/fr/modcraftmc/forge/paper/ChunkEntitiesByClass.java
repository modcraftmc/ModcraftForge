package fr.modcraftmc.forge.paper;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ChunkEntitiesByClass {

    // this class attempts to restore the original intent of nms.EntitySlice and improve upon it:
    // fast lookups for specific entity types in a chunk. However vanilla does not track things on a
    // chunk-wide basis, which is very important to our optimisations here: we want to eliminate chunks
    // before searching multiple slices. We also want to maintain only lists that we need to maintain for memory purposes:
    // so we have no choice but to lazily initialise mappings of class -> entity.
    // Typically these are used for entity AI lookups, which means we take a heavy initial cost but ultimately win
    // since AI lookups happen a lot.

    // This optimisation is only half of the battle with entity AI, we need to be smarter about picking the closest entity.
    // See World#getClosestEntity

    // aggressively high load factors for each map here + fastutil collections: we want the smallest memory footprint
    private final ExposedReference2IntOpenHashMap<Class<?>> chunkWideCount = new ExposedReference2IntOpenHashMap<>(4, 0.9f);
    {
        this.chunkWideCount.defaultReturnValue(Integer.MIN_VALUE);
    }
    private final Reference2ObjectOpenHashMap<Class<?>, ArrayList<Entity>>[] slices = new Reference2ObjectOpenHashMap[16];
    private final Chunk chunk;

    public ChunkEntitiesByClass(final Chunk chunk) {
        this.chunk = chunk;
    }

    public boolean hasEntitiesMaybe(final Class<?> clazz) {
        final int count = this.chunkWideCount.getInt(clazz);
        return count == Integer.MIN_VALUE || count > 0;
    }

    public void addEntity(final Entity entity, final int sectionY) {
        if (this.chunkWideCount.isEmpty()) {
            return;
        }

        final Object[] keys = this.chunkWideCount.getKey();
        final int[] values = this.chunkWideCount.getValue();

        Reference2ObjectOpenHashMap<Class<?>, ArrayList<Entity>> slice = this.slices[sectionY];
        if (slice == null) {
            slice = this.slices[sectionY] = new Reference2ObjectOpenHashMap<>(4, 0.9f);
        }

        for (int i = 0, len = keys.length; i < len; ++i) {
            final Object _key = keys[i];
            if (!(_key instanceof Class)) {
                continue;
            }
            final Class<?> key = (Class<?>)_key;
            if (key.isInstance(entity)) {
                ++values[i];
                slice.computeIfAbsent(key, (keyInMap) -> {
                    return new ArrayList<>();
                }).add(entity);
            }
        }
    }

    public void removeEntity(final Entity entity, final int sectionY) {
        if (this.chunkWideCount.isEmpty()) {
            return;
        }

        final Object[] keys = this.chunkWideCount.getKey();
        final int[] values = this.chunkWideCount.getValue();

        Reference2ObjectOpenHashMap<Class<?>, ArrayList<Entity>> slice = this.slices[sectionY];
        if (slice == null) {
            return; // seriously brain damaged plugins
        }

        for (int i = 0, len = keys.length; i < len; ++i) {
            final Object _key = keys[i];
            if (!(_key instanceof Class)) {
                continue;
            }
            final Class<?> key = (Class<?>)_key;
            if (key.isInstance(entity)) {
                --values[i];
                final ArrayList<Entity> list = slice.get(key);
                if (list == null) {
                    return; // seriously brain damaged plugins
                }
                list.remove(entity);
            }
        }
    }


    private void computeClass(final Class<?> clazz) {
        int totalCount = 0;

        EntityList entityList = this.chunk.entities;
        Entity[] entities = entityList.getRawData();
        for (int i = 0, len = entityList.size(); i < len; ++i) {
            final Entity entity = entities[i];

            if (clazz.isInstance(entity)) {
                ++totalCount;
                Reference2ObjectOpenHashMap<Class<?>, ArrayList<Entity>> slice = this.slices[entity.chunkCoordY];
                if (slice == null) {
                    slice = this.slices[entity.chunkCoordY] = new Reference2ObjectOpenHashMap<>(4, 0.9f);
                }
                slice.computeIfAbsent(clazz, (keyInMap) -> {
                    return new ArrayList<>();
                }).add(entity);
            }
        }

        this.chunkWideCount.put(clazz, totalCount);
    }

    public void lookupClass(final Class<?> clazz, final Entity entity, final AxisAlignedBB boundingBox, final Predicate<Entity> predicate, final List<Entity> into) {
        final int count = this.chunkWideCount.getInt(clazz);
        if (count == Integer.MIN_VALUE) {
            this.computeClass(clazz);
            if (this.chunkWideCount.getInt(clazz) <= 0) {
                return;
            }
        } else if (count <= 0) {
            return;
        }

        // copied from getEntities
        int min = MathHelper.floor((boundingBox.minY - 2.0D) / 16.0D);
        int max = MathHelper.floor((boundingBox.maxY + 2.0D) / 16.0D);

        min = MathHelper.clamp(min, 0, this.slices.length - 1);
        max = MathHelper.clamp(max, 0, this.slices.length - 1);

        for (int y = min; y <= max; ++y) {
            final Reference2ObjectOpenHashMap<Class<?>, ArrayList<Entity>> slice = this.slices[y];
            if (slice == null) {
                continue;
            }

            final ArrayList<Entity> entities = slice.get(clazz);
            if (entities == null) {
                continue;
            }

            for (int i = 0, len = entities.size(); i < len; ++i) {
                Entity entity1 = entities.get(i);
                if (entity1.removed) continue; // Paper

                if (entity1 != entity && entity1.getBoundingBox().intersects(boundingBox)) {
                    if (predicate == null || predicate.test(entity1)) {
                        into.add(entity1);
                    }
                }
            }
        }
    }

    static final class ExposedReference2IntOpenHashMap<K> extends Reference2IntOpenHashMap<K> {

        public ExposedReference2IntOpenHashMap(final int expected, final float loadFactor) {
            super(expected, loadFactor);
        }

        public Object[] getKey() {
            return this.key;
        }

        public int[] getValue() {
            return this.value;
        }
    }
}

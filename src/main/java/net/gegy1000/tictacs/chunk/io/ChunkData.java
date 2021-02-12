package net.gegy1000.tictacs.chunk.io;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.gegy1000.tictacs.PoiStorageAccess;
import net.gegy1000.tictacs.compatibility.TicTacsCompatibility;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.ITickList;
import net.minecraft.world.SerializableTickList;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.LongStream;

public final class ChunkData {
    private static final Logger LOGGER = LogManager.getLogger(ChunkData.class);

    private static final int STARLIGHT_LIGHT_VERSION = 1;

    private final ChunkPos pos;
    private final ChunkStatus status;
    private final long inhabitedTime;
    private final UpgradeData upgradeData;

    private final int[] biomeIds;

    private final ChunkSection[] sections;
    private final boolean[] sectionHasPois;

    private final ChunkLightData lightData;
    private final boolean lightOn;

    private final Map<Heightmap.Type, long[]> heightmaps;

    private final ITickList<Block> blockTickScheduler;
    private final ITickList<Fluid> fluidTickScheduler;
    private final List<BlockPos> blocksForPostProcessing;

    private final List<CompoundNBT> entityTags;
    private final List<CompoundNBT> blockEntityTags;

    private final Map<Structure<?>, CompoundNBT> structureStarts;
    private final Map<Structure<?>, LongSet> structureReferences;

    private final boolean shouldSave;

    private final ProtoData protoData;

    private ChunkData(
            ChunkPos pos, ChunkStatus status,
            long inhabitedTime, UpgradeData upgradeData,
            int[] biomeIds, ChunkSection[] sections,
            boolean[] sectionHasPois,
            ChunkLightData lightData, boolean lightOn,
            Map<Heightmap.Type, long[]> heightmaps,
            ITickList<Block> blockTickScheduler, ITickList<Fluid> fluidTickScheduler,
            List<BlockPos> blocksForPostProcessing,
            List<CompoundNBT> entityTags, List<CompoundNBT> blockEntityTags,
            Map<Structure<?>, CompoundNBT> structureStarts,
            Map<Structure<?>, LongSet> structureReferences,
            boolean shouldSave,
            ProtoData protoData
    ) {
        this.pos = pos;
        this.status = status;
        this.inhabitedTime = inhabitedTime;
        this.upgradeData = upgradeData;
        this.biomeIds = biomeIds;
        this.sections = sections;
        this.sectionHasPois = sectionHasPois;
        this.lightData = lightData;
        this.lightOn = lightOn;
        this.heightmaps = heightmaps;
        this.blockTickScheduler = blockTickScheduler;
        this.fluidTickScheduler = fluidTickScheduler;
        this.blocksForPostProcessing = blocksForPostProcessing;
        this.entityTags = entityTags;
        this.blockEntityTags = blockEntityTags;
        this.structureStarts = structureStarts;
        this.structureReferences = structureReferences;
        this.shouldSave = shouldSave;
        this.protoData = protoData;
    }

    public static ChunkData deserialize(ChunkPos chunkPos, CompoundNBT tag) {
        CompoundNBT levelTag = tag.getCompound("Level");

        ChunkStatus status = ChunkStatus.byName(levelTag.getString("Status"));

        ChunkPos serializedPos = new ChunkPos(levelTag.getInt("xPos"), levelTag.getInt("zPos"));
        if (!serializedPos.equals(chunkPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", chunkPos, chunkPos, serializedPos);
        }

        int[] biomeIds = levelTag.contains("Biomes", Constants.NBT.TAG_INT_ARRAY) ? levelTag.getIntArray("Biomes") : null;
        UpgradeData upgradeData = levelTag.contains("UpgradeData", Constants.NBT.TAG_COMPOUND) ? new UpgradeData(levelTag.getCompound("UpgradeData")) : UpgradeData.EMPTY;

        ITickList<Block> blockScheduler = new ChunkPrimerTickList<>(block -> {
            return block == null || block.getDefaultState().isAir();
        }, chunkPos, levelTag.getList("ToBeTicked",  Constants.NBT.TAG_LIST));

        ITickList<Fluid> fluidScheduler = new ChunkPrimerTickList<>(fluid -> {
            return fluid == null || fluid == Fluids.EMPTY;
        }, chunkPos, levelTag.getList("LiquidsToBeTicked", Constants.NBT.TAG_LIST));

        ChunkSection[] sections = new ChunkSection[16];
        boolean[] sectionHasPois = new boolean[16];

        boolean lightOn = levelTag.getBoolean("isLightOn");

        ChunkLightData lightData;

        if (TicTacsCompatibility.STARLIGHT_LOADED) {
            lightData = new StarlightChunkLightData();
            lightOn = levelTag.getInt("starlight.light_versiom") == STARLIGHT_LIGHT_VERSION;
        } else {
            lightData = new VanillaChunkLightData();
        }

        ListNBT sectionsList = levelTag.getList("Sections", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < sectionsList.size(); i++) {
            CompoundNBT sectionTag = sectionsList.getCompound(i);
            int sectionY = sectionTag.getByte("Y");

            if (sectionTag.contains("Palette", Constants.NBT.TAG_LIST) && sectionTag.contains("BlockStates", Constants.NBT.TAG_LONG_ARRAY)) {
                ChunkSection section = new ChunkSection(sectionY << 4);

                ListNBT palette = sectionTag.getList("Palette", Constants.NBT.TAG_COMPOUND);
                long[] data = sectionTag.getLongArray("BlockStates");
                section.getData().readChunkPalette(palette, data);

                section.recalculateRefCounts();

                if (!section.isEmpty()) {
                    sections[sectionY] = section;
                    sectionHasPois[sectionY] = section.isValidPOIState(PointOfInterestType.BLOCKS_OF_INTEREST::contains);
                }
            }

            if (lightOn) {
                lightData.acceptSection(sectionY, sectionTag, status);
            }
        }

        ChunkStatus.Type chunkType = status.getType();

        List<CompoundNBT> entityTags = new ArrayList<>();
        List<CompoundNBT> blockEntityTags = new ArrayList<>();

        ListNBT entitiesList = levelTag.getList("Entities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entitiesList.size(); i++) {
            entityTags.add(entitiesList.getCompound(i));
        }

        ListNBT blockEntitiesList = levelTag.getList("TileEntities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < blockEntitiesList.size(); i++) {
            blockEntityTags.add(blockEntitiesList.getCompound(i));
        }

        if (chunkType == ChunkStatus.Type.LEVELCHUNK) {
            if (levelTag.contains("TileTicks", Constants.NBT.TAG_LIST)) {
                blockScheduler = SerializableTickList.create(levelTag.getList("TileTicks", Constants.NBT.TAG_COMPOUND), Registry.BLOCK::getKey, Registry.BLOCK::getOrDefault);
            }

            if (levelTag.contains("LiquidTicks", Constants.NBT.TAG_LIST)) {
                fluidScheduler = SerializableTickList.create(levelTag.getList("LiquidTicks", Constants.NBT.TAG_COMPOUND), Registry.FLUID::getKey, Registry.FLUID::getOrDefault);
            }
        }

        CompoundNBT heightmapsTag = levelTag.getCompound("Heightmaps");

        Map<Heightmap.Type, long[]> heightmaps = new EnumMap<>(Heightmap.Type.class);
        for (Heightmap.Type type : status.getHeightMaps()) {
            String name = type.getString();
            if (heightmapsTag.contains(name, Constants.NBT.TAG_LONG_ARRAY)) {
                heightmaps.put(type, heightmapsTag.getLongArray(name));
            }
        }

        CompoundNBT structuresTag = levelTag.getCompound("Structures");
        Map<Structure<?>, CompoundNBT> structureStarts = deserializeStructureStarts(structuresTag);
        Map<Structure<?>, LongSet> structureReferences = deserializeStructureReferences(chunkPos, structuresTag);

        List<BlockPos> blocksForPostProcessing = new ArrayList<>();

        ListNBT postProcessingList = levelTag.getList("PostProcessing", Constants.NBT.TAG_LIST);
        for (int sectionY = 0; sectionY < postProcessingList.size(); sectionY++) {
            ListNBT queueList = postProcessingList.getList(sectionY);
            for (int i = 0; i < queueList.size(); i++) {
                BlockPos pos = ChunkPrimer.unpackToWorld(queueList.getShort(sectionY), sectionY, chunkPos);
                blocksForPostProcessing.add(pos);
            }
        }

        ProtoData protoData = null;
        if (chunkType == ChunkStatus.Type.PROTOCHUNK) {
            protoData = deserializeProtoData(chunkPos, levelTag, status, sections, lightOn);
        }

        long inhabitedTime = levelTag.getLong("InhabitedTime");
        boolean shouldSave = levelTag.getBoolean("shouldSave");

        return new ChunkData(
                chunkPos, status,
                inhabitedTime, upgradeData,
                biomeIds, sections, sectionHasPois,
                lightData, lightOn,
                heightmaps, blockScheduler, fluidScheduler,
                blocksForPostProcessing, entityTags, blockEntityTags,
                structureStarts, structureReferences,
                shouldSave,
                protoData
        );
    }

    private static Map<Structure<?>, CompoundNBT> deserializeStructureStarts(CompoundNBT tag) {
        Map<Structure<?>, CompoundNBT> starts = new Object2ObjectOpenHashMap<>();

        CompoundNBT startsTag = tag.getCompound("Starts");

        for (String key : startsTag.keySet()) {
            Structure<?> feature = Structure.NAME_STRUCTURE_BIMAP.get(key.toLowerCase(Locale.ROOT));
            if (feature == null) {
                LOGGER.error("Unknown structure start: {}", key);
                continue;
            }

            starts.put(feature, startsTag.getCompound(key));
        }

        return starts;
    }

    private static Map<Structure<?>, LongSet> deserializeStructureReferences(ChunkPos pos, CompoundNBT tag) {
        Map<Structure<?>, LongSet> references = new Object2ObjectOpenHashMap<>();
        CompoundNBT referencesTag = tag.getCompound("References");

        for (String key : referencesTag.keySet()) {
            Structure<?> feature = Structure.NAME_STRUCTURE_BIMAP.get(key.toLowerCase(Locale.ROOT));

            LongStream referenceStream = Arrays.stream(referencesTag.getLongArray(key)).filter(reference -> {
                ChunkPos chunkPos = new ChunkPos(reference);
                if (chunkPos.getChessboardDistance(pos) > 8) {
                    LOGGER.warn("Found invalid structure reference [{} @ {}] for chunk {}", key, chunkPos, pos);
                    return false;
                }
                return true;
            });

            references.put(feature, new LongOpenHashSet(referenceStream.toArray()));
        }

        return references;
    }

    private static ProtoData deserializeProtoData(ChunkPos chunkPos, CompoundNBT levelTag, ChunkStatus status, ChunkSection[] sections, boolean lightOn) {
        List<BlockPos> lightSources = new ArrayList<>();

        ListNBT lightSectionList = levelTag.getList("Lights", Constants.NBT.TAG_LIST);
        for (int sectionY = 0; sectionY < lightSectionList.size(); sectionY++) {
            ListNBT lightList = lightSectionList.getList(sectionY);
            for (int i = 0; i < lightList.size(); i++) {
                lightSources.add(ChunkPrimer.unpackToWorld(lightList.getShort(i), sectionY, chunkPos));
            }
        }

        Map<GenerationStage.Carving, BitSet> carvingMasks = new EnumMap<>(GenerationStage.Carving.class);

        CompoundNBT carvingMasksTag = levelTag.getCompound("CarvingMasks");
        for (String key : carvingMasksTag.keySet()) {
            GenerationStage.Carving carver = GenerationStage.Carving.valueOf(key);
            carvingMasks.put(carver, BitSet.valueOf(carvingMasksTag.getByteArray(key)));
        }

        if (!lightOn && status.isAtLeast(ChunkStatus.LIGHT)) {
            for (BlockPos pos : BlockPos.getAllInBoxMutable(0, 0, 0, 15, 255, 15)) {
                ChunkSection section = sections[pos.getY() >> 4];
                if (section == null) {
                    continue;
                }

                BlockState state = section.getBlockState(pos.getX(), pos.getY() & 15, pos.getZ());
                if (state.getLightValue() != 0) {
                    lightSources.add(new BlockPos(
                            chunkPos.getXStart() + pos.getX(),
                            pos.getY(),
                            chunkPos.getZStart() + pos.getZ()
                    ));
                }
            }
        }

        return new ProtoData(lightSources, carvingMasks);
    }

    public IChunk createChunk(ServerWorld world, TemplateManager structures, PointOfInterestManager poi) {
        DynamicRegistries registryManager = world.func_241828_r();
        ServerChunkProvider chunkManager = world.getChunkProvider();
        ChunkGenerator chunkGenerator = chunkManager.getChunkGenerator();
        ServerWorldLightManager lightingProvider = chunkManager.getLightManager();
        BiomeProvider biomeSource = chunkGenerator.getBiomeProvider();

        BiomeContainer biomes = null;
        if (this.biomeIds != null || this.status.isAtLeast(ChunkStatus.BIOMES)) {
            MutableRegistry<Biome> biomeRegistry = registryManager.getRegistry(Registry.BIOME_KEY);
            biomes = new BiomeContainer(biomeRegistry, this.pos, biomeSource, this.biomeIds);
        }

        this.lightData.applyToWorld(this.pos, world);

        ChunkStatus.Type chunkType = this.status.getType();

        IChunk chunk;
        ChunkPrimer protoChunk;

        if (chunkType == ChunkStatus.Type.LEVELCHUNK) {
            Chunk worldChunk = this.createWorldChunk(world, biomes);
            chunk = worldChunk;
            protoChunk = new ChunkPrimerWrapper(worldChunk);
        } else {
            protoChunk = this.createProtoChunk(lightingProvider, biomes);
            chunk = protoChunk;
        }

        for (int sectionY = 0; sectionY < this.sectionHasPois.length; sectionY++) {
            if (this.sectionHasPois[sectionY]) {
                ((PoiStorageAccess) poi).initSectionWithPois(this.pos, this.sections[sectionY]);
            }
        }

        this.populateStructures(chunk, structures, world.getSeed());
        this.populateHeightmaps(chunk);

        if (this.shouldSave) {
            chunk.setModified(true);
        }

        for (BlockPos pos : this.blocksForPostProcessing) {
            chunk.addPackedPosition(ChunkPrimer.packToLocal(pos), pos.getY() >> 4);
        }

        protoChunk.setLight(this.lightOn);

        this.lightData.applyToChunk(protoChunk);

        return protoChunk;
    }

    private void populateHeightmaps(IChunk chunk) {
        if (!this.status.isAtLeast(ChunkStatus.NOISE)) {
            return;
        }

        EnumSet<Heightmap.Type> missingHeightmaps = EnumSet.noneOf(Heightmap.Type.class);
        for (Heightmap.Type type : this.status.getHeightMaps()) {
            long[] heightmap = this.heightmaps.get(type);
            if (heightmap != null) {
                chunk.setHeightmap(type, heightmap);
            } else {
                missingHeightmaps.add(type);
            }
        }

        if (!missingHeightmaps.isEmpty()) {
            Heightmap.updateChunkHeightmaps(chunk, missingHeightmaps);
        }
    }

    private void populateStructures(IChunk chunk, TemplateManager structures, long worldSeed) {
        Map<Structure<?>, StructureStart<?>> structureStarts = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<Structure<?>, CompoundNBT> entry : this.structureStarts.entrySet()) {
            StructureStart<?> start = Structure.deserializeStructureStart(structures, entry.getValue(), worldSeed);
            if (start != null) {
                structureStarts.put(entry.getKey(), start);
            }
        }

        chunk.setStructureStarts(structureStarts);
        chunk.setStructureReferences(this.structureReferences);
    }

    private Chunk createWorldChunk(ServerWorld world, BiomeContainer biomes) {
        List<CompoundNBT> entityTags = this.entityTags;
        List<CompoundNBT> blockEntityTags = this.blockEntityTags;
        Consumer<Chunk> loadToWorld = worldChunk -> addEntitiesToWorldChunk(worldChunk, entityTags, blockEntityTags);

        return new Chunk(
                world, this.pos, biomes, this.upgradeData,
                this.blockTickScheduler, this.fluidTickScheduler,
                this.inhabitedTime,
                this.sections,
                loadToWorld
        );
    }

    private ChunkPrimer createProtoChunk(ServerWorldLightManager lightingProvider, BiomeContainer biomes) {
        ChunkPrimer chunk = new ChunkPrimer(
                this.pos, this.upgradeData,
                this.sections,
                (ChunkPrimerTickList<Block>) this.blockTickScheduler,
                (ChunkPrimerTickList<Fluid>) this.fluidTickScheduler
        );

        chunk.setBiomes(biomes);
        chunk.setInhabitedTime(this.inhabitedTime);
        chunk.setStatus(this.status);

        if (this.status.isAtLeast(ChunkStatus.FEATURES)) {
            chunk.setLightManager(lightingProvider);
        }

        for (CompoundNBT tag : this.entityTags) {
            chunk.addEntity(tag);
        }

        for (CompoundNBT tag : this.blockEntityTags) {
            chunk.addTileEntity(tag);
        }

        Preconditions.checkNotNull(this.protoData, "loaded no proto data for ProtoChunk");

        for (BlockPos pos : this.protoData.lightSources) {
            chunk.addLightPosition(pos);
        }

        for (Map.Entry<GenerationStage.Carving, BitSet> entry : this.protoData.carvingMasks.entrySet()) {
            chunk.setCarvingMask(entry.getKey(), entry.getValue());
        }

        return chunk;
    }

    private static void addEntitiesToWorldChunk(Chunk chunk, List<CompoundNBT> entityTags, List<CompoundNBT> blockEntityTags) {
        World world = chunk.getWorld();
        for (CompoundNBT tag : entityTags) {
            EntityType.loadEntityAndExecute(tag, world, entity -> {
                chunk.addEntity(entity);
                return entity;
            });
            chunk.setHasEntities(true);
        }

        for (CompoundNBT tag : blockEntityTags) {
            if (!tag.getBoolean("keepPacked")) {
                BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                TileEntity entity = TileEntity.readTileEntity(chunk.getBlockState(pos), tag);
                if (entity != null) {
                    chunk.addTileEntity(entity);
                }
            } else {
                chunk.addTileEntity(tag);
            }
        }
    }

    private static class ProtoData {
        final List<BlockPos> lightSources;
        final Map<GenerationStage.Carving, BitSet> carvingMasks;

        ProtoData(List<BlockPos> lightSources, Map<GenerationStage.Carving, BitSet> carvingMasks) {
            this.lightSources = lightSources;
            this.carvingMasks = carvingMasks;
        }
    }
}

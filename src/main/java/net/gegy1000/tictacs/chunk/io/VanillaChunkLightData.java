package net.gegy1000.tictacs.chunk.io;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraftforge.common.util.Constants;

public final class VanillaChunkLightData implements ChunkLightData {
    private NibbleArray[] blockLightSections;
    private NibbleArray[] skyLightSections;

    @Override
    public void acceptSection(int sectionY, CompoundNBT sectionTag, ChunkStatus status) {
        if (sectionTag.contains("BlockLight", Constants.NBT.TAG_BYTE_ARRAY)) {
            this.putBlockSection(sectionY, sectionTag.getByteArray("BlockLight"));
        }

        if (sectionTag.contains("SkyLight", Constants.NBT.TAG_BYTE_ARRAY)) {
            this.putSkySection(sectionY, sectionTag.getByteArray("SkyLight"));
        }
    }

    private void putBlockSection(int y, byte[] data) {
        NibbleArray[] blockLightSections = this.blockLightSections;
        if (blockLightSections == null) {
            this.blockLightSections = blockLightSections = new NibbleArray[18];
        }

        blockLightSections[y + 1] = new NibbleArray(data);
    }

    private void putSkySection(int y, byte[] data) {
        NibbleArray[] skyLightSections = this.skyLightSections;
        if (skyLightSections == null) {
            this.skyLightSections = skyLightSections = new NibbleArray[18];
        }

        skyLightSections[y + 1] = new NibbleArray(data);
    }

    @Override
    public void applyToWorld(ChunkPos chunkPos, ServerWorld world) {
        NibbleArray[] blockLightSections = this.blockLightSections;
        NibbleArray[] skyLightSections = this.skyLightSections;
        if (blockLightSections == null && skyLightSections == null) {
            return;
        }

        ServerWorldLightManager lightingProvider = world.getChunkProvider().getLightManager();
        lightingProvider.retainData(chunkPos, true);

        boolean hasSkylight = world.getDimensionType().hasSkyLight();
        for (int sectionY = -1; sectionY < 17; sectionY++) {
            SectionPos sectionPos = SectionPos.from(chunkPos, sectionY);

            NibbleArray blockLight = blockLightSections != null ? blockLightSections[sectionY + 1] : null;
            if (blockLight != null) {
                lightingProvider.setData(LightType.BLOCK, sectionPos, blockLight, true);
            }

            NibbleArray skyLight = skyLightSections != null ? skyLightSections[sectionY + 1] : null;
            if (hasSkylight && skyLight != null) {
                lightingProvider.setData(LightType.SKY, sectionPos, skyLight, true);
            }
        }
    }

    @Override
    public void applyToChunk(ChunkPrimer chunk) {
    }
}

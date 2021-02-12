package net.gegy1000.tictacs.chunk.io;

import ca.spottedleaf.starlight.common.chunk.ExtendedChunk;
import ca.spottedleaf.starlight.common.light.SWMRNibbleArray;
import ca.spottedleaf.starlight.common.light.StarLightEngine;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;

public final class StarlightChunkLightData implements ChunkLightData {

    private final SWMRNibbleArray[] blockLightSections = StarLightEngine.getFilledEmptyLight();
    private final SWMRNibbleArray[] skyLightSections = StarLightEngine.getFilledEmptyLight();

    @Override
    public void acceptSection(int y, CompoundNBT sectionTag, ChunkStatus status) {
        if (!status.isAtLeast(ChunkStatus.LIGHT)) {
            return;
        }

        if (sectionTag.contains("BlockLight", Constants.NBT.TAG_BYTE_ARRAY)) {
            this.blockLightSections[y + 1] = new SWMRNibbleArray(sectionTag.getByteArray("BlockLight").clone());
        }

        if (sectionTag.contains("SkyLight", Constants.NBT.TAG_BYTE_ARRAY)) {
            this.skyLightSections[y + 1] = new SWMRNibbleArray(sectionTag.getByteArray("SkyLight").clone());
        } else if (sectionTag.getBoolean("starlight.skylight_uninit")) {
            this.skyLightSections[y + 1] = new SWMRNibbleArray();
        }
    }

    @Override
    public void applyToWorld(ChunkPos chunkPos, ServerWorld world) {
    }

    @Override
    public void applyToChunk(ChunkPrimer chunk) {
        ExtendedChunk nibbledChunk = (ExtendedChunk) chunk;
        nibbledChunk.setBlockNibbles(this.blockLightSections);
        nibbledChunk.setSkyNibbles(this.skyLightSections);
    }


}
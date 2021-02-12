package net.gegy1000.tictacs.chunk.step;

import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;

import java.util.List;

public final class ChunkStepContext {
    public final ChunkController controller;
    public final ChunkEntry entry;
    public final ServerWorld world;
    public final ChunkGenerator generator;
    public final TemplateManager structures;
    public final ServerWorldLightManager lighting;
    public final IChunk chunk;
    public final List<IChunk> chunks;

    private WorldGenRegion region;
    private StructureManager structureAccessor;

    public ChunkStepContext(ChunkController controller, ChunkEntry entry, ServerWorld world, ChunkGenerator generator, TemplateManager structures, ServerWorldLightManager lighting, IChunk chunk, List<IChunk> chunks) {
        this.controller = controller;
        this.entry = entry;
        this.world = world;
        this.generator = generator;
        this.structures = structures;
        this.lighting = lighting;
        this.chunk = chunk;
        this.chunks = chunks;
    }

    public WorldGenRegion asRegion() {
        if (this.region == null) {
            this.region = new WorldGenRegion(this.world, this.chunks);
        }
        return this.region;
    }

    public StructureManager asStructureAccessor() {
        if (this.structureAccessor == null) {
            WorldGenRegion region = this.asRegion();
            this.structureAccessor = this.world.func_241112_a_().getStructureManager(region);
        }
        return this.structureAccessor;
    }
}

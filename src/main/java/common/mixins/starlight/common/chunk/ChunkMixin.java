package common.mixins.starlight.common.chunk;

import ca.spottedleaf.starlight.common.chunk.ExtendedChunk;
import net.minecraft.world.chunk.IChunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IChunk.class)
public interface ChunkMixin extends ExtendedChunk {}
package common.mixins.tictacs.threading_fix;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.stream.Stream;

@Mixin(WorldGenRegion.class)
public class ChunkRegionMixin {
    @Shadow
    @Final
    private ServerWorld world;

    @Unique
    private StructureManager structureAccess;

    /**
     * @reason vanilla calls getStructures on the main world object. we don't want to do this! this can cause a race
     * condition where both the main thread and the worker threads are blocking on a chunk to load.
     * @author gegy1000
     */
    @Overwrite
    public Stream<? extends StructureStart<?>> func_241827_a(SectionPos pos, Structure<?> feature) {
        if (this.structureAccess == null) {
            this.structureAccess = this.world.func_241112_a_().getStructureManager((WorldGenRegion) (Object) this);
        }

        return this.structureAccess.func_235011_a_(pos, feature);
    }
}

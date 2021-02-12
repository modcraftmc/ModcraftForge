package common.mixins.tictacs.io;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import net.gegy1000.tictacs.PoiStorageAccess;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DefaultTypeReferences;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.village.PointOfInterestData;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.storage.RegionSectionCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Mixin(PointOfInterestManager.class)
public abstract class PointOfInterestStorageMixin extends RegionSectionCache<PointOfInterestData> implements PoiStorageAccess {


    public PointOfInterestStorageMixin(File p_i231897_1_, Function<Runnable, Codec<PointOfInterestData>> p_i231897_2_, Function<Runnable, PointOfInterestData> p_i231897_3_, DataFixer p_i231897_4_, DefaultTypeReferences p_i231897_5_, boolean p_i231897_6_) {
        super(p_i231897_1_, p_i231897_2_, p_i231897_3_, p_i231897_4_, p_i231897_5_, p_i231897_6_);
    }

    @Shadow
    protected abstract void updateFromSelection(ChunkSection section, SectionPos sectionPos, BiConsumer<BlockPos, PointOfInterestType> add);

    @Override
    public void initSectionWithPois(ChunkPos pos, ChunkSection section) {
        SectionPos sectionPos = SectionPos.from(pos, section.getYLocation() >> 4);
        Util.acceptOrElse(this.func_219113_d(sectionPos.asLong()), set -> {
            set.refresh(add -> this.updateFromSelection(section, sectionPos, add));
        }, () -> {
            PointOfInterestData set = this.func_235995_e_(sectionPos.asLong());
            this.updateFromSelection(section, sectionPos, set::add);
        });
    }
}

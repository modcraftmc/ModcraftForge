package org.yatopia.server;

import me.jellysquid.mods.lithium.common.util.Producer;
import net.minecraft.util.math.CubeCoordinateIterator;
import net.minecraft.util.math.SectionPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class YatopiaChunkSectionPos {

    public static List<SectionPos> getChunkSectionPosList(int i, int j, int k, int l, int i1, int j1) {
        List<SectionPos> list = new ArrayList<>();
        Producer.fillList(getChunkSectionPosProducer(i, j, k, l, i1, j1), list);
        return list;
    }

    private static Producer<SectionPos> getChunkSectionPosProducer(int i, int j, int k, int l, int i1, int j1) {
        return new Producer<SectionPos>() {
            final CubeCoordinateIterator cursorPos = new CubeCoordinateIterator(i, j, k, l, i1, j1);

            @Override
            public boolean computeNext(Consumer<? super SectionPos> consumer) {
                if (cursorPos.hasNext()) {
                    consumer.accept(SectionPos.of(cursorPos.getX(), cursorPos.getY(), cursorPos.getZ()));
                    return true;
                }
                return false;
            }
        };
    }
}

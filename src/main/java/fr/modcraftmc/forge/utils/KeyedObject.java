package fr.modcraftmc.forge.utils;

import net.minecraft.util.ResourceLocation;

public interface KeyedObject {

    ResourceLocation getMinecraftKey();
    default String getMinecraftKeyString() {
        ResourceLocation key = getMinecraftKey();
        return key != null ? key.toString() : null;
    }
}

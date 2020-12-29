package fr.modcraftmc.forge.spigot;

import net.minecraft.item.ItemStack;

public class SpeedboostUtils {

    public static int tickCount;
    public static int itemDirtyTicks = 20;

    public static boolean fastMatches(ItemStack itemstack, ItemStack itemstack1) {
        if (itemstack.isEmpty() && itemstack1.isEmpty()) {
            return true;
        }
        if (!itemstack.isEmpty() && !itemstack1.isEmpty()) {
            return itemstack.getCount() == itemstack1.getCount() && itemstack.getItem() == itemstack1.getItem() && itemstack.getDamage() == itemstack1.getDamage();
        }
        return false;
    }
}

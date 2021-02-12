package fr.modcraftmc.forge.api;

import fr.modcraftforge.forge.ModcraftForge;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class ModcraftForgeAPI {

    private static ModcraftForgeAPI instance;

    //launching part

    public static String getLoadingTime() {
        return ModcraftForge.getFormatedStartTime();
    }

    //networking part



    public static ModcraftForgeAPI getAPI()
    {
        return instance == null ? instance = new ModcraftForgeAPI() : instance;
    }
}

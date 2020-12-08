package fr.modcraftmc.modcraftforge.api;

import fr.modcraftforge.ModcraftForge;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class ModcraftForgeAPI {

    private static ModcraftForgeAPI instance;

    //launching part

    public static String getLoadingTime() {
        return ModcraftForge.getFormatedStartTime();
    }

    //networking part

    private ITextComponent networkPrefix = new StringTextComponent("");
    

    public static void setMessagePrefix(ITextComponent component)
    {
        getAPI().networkPrefix = component.appendText("\n \n");
    }

    public static ITextComponent getMessagePrefix() {
        return getAPI().networkPrefix;
    }

    public static ModcraftForgeAPI getAPI()
    {
        return instance == null ? instance = new ModcraftForgeAPI() : instance;
    }
}

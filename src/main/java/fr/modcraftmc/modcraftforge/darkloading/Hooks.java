package fr.modcraftmc.modcraftforge.darkloading;

import net.minecraft.util.math.MathHelper;

public class Hooks {


    public static int backColor = 2829099 | 0xff000000,
            progStartColor = 16711680 | 0xff000000,
            progEndColor = 65280 | 0xff000000;


    public static int getProgressColor(float progress){

        int r1 = (progStartColor >> 16) & 0xff;
        int g1 = (progStartColor >> 8) & 0xff;
        int b1 = progStartColor & 0xff;

        int r2 = (progEndColor >> 16) & 0xff;
        int g2 = (progEndColor >> 8) & 0xff;
        int b2 = progEndColor & 0xff;

        int r = (int) MathHelper.lerp(progress, r1, r2) << 16;
        int g =	(int)MathHelper.lerp(progress, g1, g2) << 8;
        int b =	(int)MathHelper.lerp(progress, b1, b2);

        return 0xff000000| r | g | b;
    }

    public static int getBarBackgroundColor(float progress){

        float a = 1 - progress;

        int r = Math.round(a * 255F) << 16;
        int g = Math.round(a * 255F) << 8;
        int b = Math.round(a * 255F);

        return 0xff000000 | r | g | b;
    }

}

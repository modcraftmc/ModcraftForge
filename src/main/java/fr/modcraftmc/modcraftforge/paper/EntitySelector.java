package fr.modcraftmc.modcraftforge.paper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.function.Predicate;

public class EntitySelector {

    public static Predicate<Entity> canAITarget() { return e; } // Paper - OBFHELPER
    public static final Predicate<Entity> e = (entity) -> {
        return !(entity instanceof PlayerEntity) || !entity.isSpectator() && !((PlayerEntity) entity).isCreative();
    };
}

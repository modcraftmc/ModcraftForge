package fr.modcraftmc.forge.spigot;

public interface IActivationEntity {
    void inactiveTick();

    byte getActivationType();

    long getActivatedTick();

    void setActivatedTick(long tick);

    boolean defaultActivationState();
}

package fr.modcraftmc.modcraftforge.theading;

public class ThreadSettings {

    public enum Type {
        NORMAL, CACHED
    }

    public String name;
    public Type type;
    public int threads;

    public ThreadSettings(String name, Type type, int threads) {
        this.name = name;
        this.type = type;
        this.threads = threads;
    }
}

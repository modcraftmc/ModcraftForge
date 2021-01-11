package fr.modcraftmc.forge.threading;

public class ThreadSettings {

    public String name;
    public Type type;
    public int threads;

    public ThreadSettings(String name, Type type, int threads) {
        this.name = name;
        this.type = type;
        this.threads = threads;
    }

    public static enum Type {
        NORMAL,
        CACHED;

        private Type() {
        }
    }
}

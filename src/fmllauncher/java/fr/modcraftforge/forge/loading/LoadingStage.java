package fr.modcraftforge.forge.loading;

public class LoadingStage {

    private static String currentModLoading = "";
    private static int currentIndex, indexSize;

    public static void push(String modid, int currentindex, int size) {
        currentModLoading = modid;
        currentIndex = currentindex;
        indexSize = size;
    }

    public static String getCurrentModLoading() {
        return currentModLoading;
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static int getIndexSize() {
        return indexSize;
    }
}

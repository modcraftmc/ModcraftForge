package ca.starlight.common.blockstate;

public interface ExtendedAbstractBlockState {

    public boolean isConditionallyFullOpaque();

    public int getOpacityIfCached();

}

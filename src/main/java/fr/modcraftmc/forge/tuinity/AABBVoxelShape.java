package fr.modcraftmc.forge.tuinity;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

import java.util.ArrayList;
import java.util.List;

public class AABBVoxelShape extends VoxelShape {

    public final AxisAlignedBB aabb;

    public AABBVoxelShape(AxisAlignedBB aabb) {
        super(VoxelShapes.FULL_CUBE.getShape());
        this.aabb = aabb;
    }

    @Override
    public boolean isEmpty() {
        return this.aabb.isEmpty();
    }

    @Override
    public double getStart(Direction.Axis axis) {
        switch (axis.ordinal()) {
            case 0:
                return this.aabb.minX;
            case 1:
                return this.aabb.minY;
            case 2:
                return this.aabb.minZ;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    public double getEnd(Direction.Axis axis) {
        switch (axis.ordinal()) {
            case 0:
                return this.aabb.maxX;
            case 1:
                return this.aabb.maxY;
            case 2:
                return this.aabb.maxZ;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return this.aabb;
    }

    @Override
    protected double getValueUnchecked(Direction.Axis axis, int index) {
        switch (axis.ordinal() | (index << 2)) {
            case (0 | (0 << 2)):
                return this.aabb.minX;
            case (1 | (0 << 2)):
                return this.aabb.minY;
            case (2 | (0 << 2)):
                return this.aabb.minZ;
            case (0 | (1 << 2)):
                return this.aabb.maxX;
            case (1 | (1 << 2)):
                return this.aabb.maxY;
            case (2 | (1 << 2)):
                return this.aabb.maxZ;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    private DoubleList cachedListX;
    private DoubleList cachedListY;
    private DoubleList cachedListZ;


    @Override
    protected DoubleList getValues(Direction.Axis enumdirection_enumaxis) { // getPoints
        switch (enumdirection_enumaxis.ordinal()) {
            case 0:
                return this.cachedListX == null ? this.cachedListX = DoubleArrayList.wrap(new double[] { this.aabb.minX, this.aabb.maxX }) : this.cachedListX;
            case 1:
                return this.cachedListY == null ? this.cachedListY = DoubleArrayList.wrap(new double[] { this.aabb.minY, this.aabb.maxY }) : this.cachedListY;
            case 2:
                return this.cachedListZ == null ? this.cachedListZ = DoubleArrayList.wrap(new double[] { this.aabb.minZ, this.aabb.maxZ }) : this.cachedListZ;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    public VoxelShape withOffset(double xOffset, double yOffset, double zOffset) {
        return new AABBVoxelShape(this.aabb.offset(xOffset, yOffset, zOffset));
    }

    @Override
    public VoxelShape simplify() {
        return this;
    }

    @Override
    public void forEachBox(VoxelShapes.ILineConsumer action) {
        action.consume(this.aabb.minX, this.aabb.minY, this.aabb.minZ, this.aabb.maxX, this.aabb.maxY, this.aabb.maxZ);
    }


    @Override
    public List<AxisAlignedBB> toBoundingBoxList() {
        List<AxisAlignedBB> ret = new ArrayList<>(1);
        ret.add(this.aabb);
        return ret;
    }

    @Override
    protected int getClosestIndex(Direction.Axis axis, double position) {
        switch (axis.ordinal()) {
            case 0:
                return position < this.aabb.maxX ? (position < this.aabb.minX ? -1 : 0) : 1;
            case 1:
                return position < this.aabb.maxY ? (position < this.aabb.minY ? -1 : 0) : 1;
            case 2:
                return position < this.aabb.maxZ ? (position < this.aabb.minZ ? -1 : 0) : 1;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    protected boolean contains(double x, double y, double z) {
        return this.aabb.contains(x, y, z);
    }

    @Override
    public VoxelShape project(Direction side) {
        return super.project(side);
    }

    @Override
    public double getAllowedOffset(Direction.Axis movementAxis, AxisAlignedBB collisionBox, double desiredOffset) {

        switch (movementAxis.ordinal()) {
            case 0:
                return AxisAlignedBB.collideX(this.aabb, collisionBox, desiredOffset);
            case 1:
                return AxisAlignedBB.collideY(this.aabb, collisionBox, desiredOffset);
            case 2:
                return AxisAlignedBB.collideZ(this.aabb, collisionBox, desiredOffset);
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }
}

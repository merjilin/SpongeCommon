/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.optimization.block;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mixin(value = BlockRedstoneWire.class, priority = 1001)
public abstract class MixinBlockRedstoneWire extends Block {

    protected MixinBlockRedstoneWire(Material materialIn) {
        super(materialIn);
    }

    /** Positions that need to be turned off **/
    private List<BlockPos> turnOff = Lists.<BlockPos>newArrayList();
    /** Positions that need to be checked to be turned on **/
    private List<BlockPos> turnOn = Lists.<BlockPos>newArrayList();
    /** Positions of wire that was updated already (Ordering determines update order and is therefore required!) **/
    private final Set<BlockPos> updatedRedstoneWire = Sets.<BlockPos>newLinkedHashSet();
     
    /** Ordered arrays of the facings; Needed for the update order.
     *  I went with a vertical-first order here, but vertical last would work to.
     *  However it should be avoided to update the vertical axis between the horizontal ones as this would cause unneeded directional behavior. **/
    private static final EnumFacing[] facingsHorizontal = {EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH};
    private static final EnumFacing[] facingsVertical = {EnumFacing.DOWN, EnumFacing.UP};
    private static final EnumFacing[] facings = ArrayUtils.addAll(facingsVertical, facingsHorizontal);

    /** Offsets for all surrounding blocks that need to receive updates **/
    private static final Vec3i[] surroundingBlocksOffset;
    static {
        Set<Vec3i> set = Sets.<Vec3i>newLinkedHashSet();
        for (EnumFacing facing : facings) {
            set.add(facing.directionVec);
        }
        for (EnumFacing facing1 : facings) {
            Vec3i v1 = facing1.directionVec;
            for (EnumFacing facing2 : facings) {
                Vec3i v2 = facing2.directionVec;
                // TODO Adding an add-method to Vec3i would be nicer of course
                set.add(new Vec3i(v1.getX() + v2.getX(), v1.getY() + v2.getY(), v1.getZ() + v2.getZ()));
            }
        }
        set.remove(new Vec3i(0, 0, 0));
        surroundingBlocksOffset = set.toArray(new Vec3i[set.size()]);
    }

    @Shadow public boolean canProvidePower;
    @Shadow public abstract int getMaxCurrentStrength(World worldIn, BlockPos pos, int strength);
    @Shadow public abstract boolean isPowerSourceAt(IBlockAccess worldIn, BlockPos pos, EnumFacing side);

    @Inject(method = "updateSurroundingRedstone", at = @At("HEAD"), cancellable = true)
    private void onUpdateSurroundingRedstone(World worldIn, BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
        if (!worldIn.isRemote) {
            this.updateSurroundingRedstone(worldIn, pos);
            cir.setReturnValue(state);
        }
    }

    @Inject(method = "calculateCurrentChanges", at = @At("HEAD"), cancellable = true)
    private void onCalculateCurrentChanges(World worldIn, BlockPos pos1, BlockPos pos2, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
        if (!worldIn.isRemote) {
            this.calculateCurrentChanges(worldIn, pos1);
            cir.setReturnValue(state);
        }
    }

    /**
     * Recalculates all surrounding wires and causes all needed updates
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position that needs updating
     */
    private void updateSurroundingRedstone(World worldIn, BlockPos pos) {
        // Recalculate the connected wires
        this.calculateCurrentChanges(worldIn, pos);

        // Set to collect all the updates, to only execute them once. Ordering required.
        Set<BlockPos> blocksNeedingUpdate = Sets.newLinkedHashSet();

        // Add the needed updates
        for (BlockPos posi : this.updatedRedstoneWire) {
            this.addBlocksNeedingUpdate(worldIn, posi, blocksNeedingUpdate);
        }
        // Add all other updates to keep known behaviors
        // They are added in a backwards order because it preserves a commonly used behavior with the update order
        Iterator<BlockPos> it = Lists.<BlockPos>newLinkedList(this.updatedRedstoneWire).descendingIterator();
        while (it.hasNext()) {
            this.addAllSurroundingBlocks(it.next(), blocksNeedingUpdate);
        }
        // Remove updates on the wires as they just were updated
        blocksNeedingUpdate.removeAll(this.updatedRedstoneWire);
        /*
         * Avoid unnecessary updates on the just updated wires A huge scale test
         * showed about 40% more ticks per second It's probably less in normal
         * usage but likely still worth it
         */
        this.updatedRedstoneWire.clear();

        // Execute updates
        for (BlockPos posi : blocksNeedingUpdate) {
            worldIn.notifyNeighborsOfStateChange(posi, (BlockRedstoneWire) (Object) this, false);
        }
    }

    /**
     * Turns on or off all connected wires
     * 
     * @param worldIn World
     * @param position Position of the wire that received the update
     */
    private void calculateCurrentChanges(World worldIn, BlockPos position) {
        // Turn off all connected wires first if needed
        if (worldIn.getBlockState(position).getBlock() == (BlockRedstoneWire) (Object) this) {
            turnOff.add(position);
        } else {
            // In case this wire was removed, check the surrounding wires
            this.checkSurroundingWires(worldIn, position);
        }

        while (!turnOff.isEmpty()) {
            BlockPos pos = turnOff.remove(0);
            IBlockState state = worldIn.getBlockState(pos);
            int oldPower = ((Integer) state.getValue(BlockRedstoneWire.POWER)).intValue();
            this.canProvidePower = false;
            int blockPower = worldIn.isBlockIndirectlyGettingPowered(pos);
            this.canProvidePower = true;
            int wirePower = this.getSurroundingWirePower(worldIn, pos);
            // Lower the strength as it moved a block
            wirePower--;
            int newPower = Math.max(blockPower, wirePower);

            // Power lowered?
            if (newPower < oldPower) {
                // If it's still powered by a direct source (but weaker) mark for turn on
                if (blockPower > 0 && !this.turnOn.contains(pos)) {
                    this.turnOn.add(pos);
                }
                // Set all the way to off for now, because wires that were powered by this need to update first
                setWireState(worldIn, pos, state, 0);
            // Power rose?
            } else if (newPower > oldPower) {
                // Set new Power
                this.setWireState(worldIn, pos, state, newPower);
            }
            // Check if surrounding wires need to change based on the current/new state and add them to the lists
            this.checkSurroundingWires(worldIn, pos);
        }
        // Now all needed wires are turned off. Time to turn them on again if there is a power source.
        while (!this.turnOn.isEmpty()) {
            BlockPos pos = this.turnOn.remove(0);
            IBlockState state = worldIn.getBlockState(pos);
            int oldPower = ((Integer) state.getValue(BlockRedstoneWire.POWER)).intValue();
            this.canProvidePower = false;
            int blockPower = worldIn.isBlockIndirectlyGettingPowered(pos);
            this.canProvidePower = true;
            int wirePower = this.getSurroundingWirePower(worldIn, pos);
            // Lower the strength as it moved a block
            wirePower--;
            int newPower = Math.max(blockPower, wirePower);

            if (newPower > oldPower) {
                setWireState(worldIn, pos, state, newPower);
            } else if (newPower < oldPower) {
                // Add warning
            }
            // Check if surrounding wires need to change based on the current/new state and add them to the lists
            this.checkSurroundingWires(worldIn, pos);
        }
        this.turnOff.clear();
        this.turnOn.clear();
    }

    /**
     * Checks if an wire needs to be marked for update depending on the power next to it
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position of the wire that might need to change
     * @param otherPower Power of the wire next to it
     */
    private void addWireToList(World worldIn, BlockPos pos, int otherPower) {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() == (BlockRedstoneWire) (Object) this) {
            int power = ((Integer) state.getValue(BlockRedstoneWire.POWER)).intValue();
            // Could get powered stronger by the neighbor?
            if (power < (otherPower - 1) && !this.turnOn.contains(pos)) {
                // Mark for turn on check.
                this.turnOn.add(pos);
            }
            // Should have powered the neighbor? Probably was powered by it and is in turn off phase.
            if (power > otherPower && !this.turnOff.contains(pos)) {
                // Mark for turn off check.
                this.turnOff.add(pos);
            }
        }
    }

    /**
     * Checks if the wires around need to get updated depending on this wires state.
     * Checks all wires below before the same layer before on top to keep
     * some more rotational symmetry around the y-axis. 
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position of the wire
     */
    private void checkSurroundingWires(World worldIn, BlockPos pos) {
        IBlockState state = worldIn.getBlockState(pos);
        int ownPower = 0;
        if (state.getBlock() == (BlockRedstoneWire) (Object) this) {
            ownPower = ((Integer) state.getValue(BlockRedstoneWire.POWER)).intValue();
        }
        // Check wires on the same layer first as they appear closer to the wire
        for (EnumFacing facing : facingsHorizontal) {
            BlockPos offsetPos = pos.offset(facing);
            if (facing.getAxis().isHorizontal()) {
                this.addWireToList(worldIn, offsetPos, ownPower);
            }
        }
        for (EnumFacing facingVertical : facingsVertical) {
            BlockPos offsetPos = pos.offset(facingVertical);
            boolean solidBlock = worldIn.getBlockState(offsetPos).isBlockNormalCube();
            for (EnumFacing facingHorizontal : facingsHorizontal) {
                // wire can travel upwards if the block on top doesn't cut the wire (is non-solid)
                // it can travel down if the block below is solid and the block "diagonal" doesn't cut off the wire (is non-solid) 
                if ((facingVertical == EnumFacing.UP && !solidBlock) || (facingVertical == EnumFacing.DOWN && solidBlock && !worldIn.getBlockState(offsetPos.offset(facingHorizontal)).isBlockNormalCube())) {
                    this.addWireToList(worldIn, offsetPos.offset(facingHorizontal), ownPower);
                }
            }
        }
    }

    /**
     * Gets the maximum power of the surrounding wires
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position of the asking wire
     * @return The maximum power of the wires that could power the wire at pos
     */
    private int getSurroundingWirePower(World worldIn, BlockPos pos) {
        int wirePower = 0;
        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) {
            BlockPos offsetPos = pos.offset(enumfacing);
            // Wires on the same layer
            wirePower = this.getMaxCurrentStrength(worldIn, offsetPos, wirePower);
            
            // Block below the wire need to be solid (Upwards diode of slabs/stairs/glowstone) and no block should cut the wire
            if(worldIn.getBlockState(offsetPos).isNormalCube() && !worldIn.getBlockState(pos.up()).isNormalCube()) {
                wirePower = this.getMaxCurrentStrength(worldIn, offsetPos.up(), wirePower);
                // Only get from power below if no block is cutting the wire
            } else if (!worldIn.getBlockState(offsetPos).isNormalCube()) {
                wirePower = this.getMaxCurrentStrength(worldIn, offsetPos.down(), wirePower);
            }
        }
        return wirePower;
    }

    /**
     * Adds all blocks that need to receive an update from a redstone change in this position.
     * This means only blocks that actually could change.
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position of the wire
     * @param set Set to add the update positions too
     */
    private void addBlocksNeedingUpdate(World worldIn, BlockPos pos, Set<BlockPos> set) {
        List<EnumFacing> connectedSides = this.getSidesToPower(worldIn, pos);
        // Add the blocks next to the wire first (closest first order)
        for (EnumFacing facing : facings) {
            BlockPos offsetPos = pos.offset(facing);
            // canConnectTo() is not the nicest solution here as it returns true for e.g. the front of a repeater
            // canBlockBePowereFromSide catches these cases
            if (connectedSides.contains(facing.getOpposite()) || facing == EnumFacing.DOWN
                    || (facing.getAxis().isHorizontal() && BlockRedstoneWire.canConnectTo(worldIn.getBlockState(offsetPos), facing))) {
                if (this.canBlockBePoweredFromSide(worldIn.getBlockState(offsetPos), facing, true))
                    set.add(offsetPos);
            }
        }
        // Later add blocks around the surrounding blocks that get powered
        for (EnumFacing facing : facings) {
            BlockPos offsetPos = pos.offset(facing);
            if (connectedSides.contains(facing.getOpposite()) || facing == EnumFacing.DOWN) {
                if (worldIn.getBlockState(offsetPos).isNormalCube()) {
                    for (EnumFacing facing1 : facings) {
                        if (this.canBlockBePoweredFromSide(worldIn.getBlockState(offsetPos.offset(facing1)), facing1, false))
                            set.add(offsetPos.offset(facing1));
                    }
                }
            }
        }
    }

    /**
     * Checks if a block can get powered from a side.
     * This behavior would better be implemented per block type as follows:
     *  - return false as default. (blocks that are not affected by redstone don't need to be updated, it doesn't really hurt if they are either)
     *  - return true for all blocks that can get powered from all side and change based on it (doors, fence gates, trap doors, note blocks, lamps, dropper, hopper, TNT, rails, possibly more)
     *  - implement own logic for pistons, repeaters, comparators and redstone torches
     *  The current implementation was chosen to keep everything in one class.
     *  
     *  Why is this extra check needed?
     *  1. It makes sure that many old behaviors still work (QC + Pistons).
     *  2. It prevents updates from "jumping".
     *     Or rather it prevents this wire to update a block that would get powered by the next one of the same line.
     *     This is to prefer as it makes understanding the update order of the wire really easy. The signal "travels" from the power source.
     * 
     * @author panda
     * 
     * @param state      State of the block
     * @param side       Side from which it gets powered
     * @param isWire     True if it's powered by a wire directly, False if through a block
     * @return           True if the block can change based on the power level it gets on the given side, false otherwise
     */
    private boolean canBlockBePoweredFromSide(IBlockState state, EnumFacing side, boolean isWire) {
        if (state.getBlock() instanceof BlockPistonBase && state.getValue(BlockPistonBase.FACING) == side.getOpposite()) {
            return false;
        }
        if (state.getBlock() instanceof BlockRedstoneDiode && state.getValue(BlockRedstoneDiode.FACING) != side.getOpposite()) {
            if (isWire && state.getBlock() instanceof BlockRedstoneComparator
                    && state.getValue(BlockRedstoneComparator.FACING).getAxis() != side.getAxis() && side.getAxis().isHorizontal()) {
                return true;
            }
            return false;
        }
        if (state.getBlock() instanceof BlockRedstoneTorch) {
            if (isWire || state.getValue(BlockRedstoneTorch.FACING) != side) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a list of all horizontal sides that can get powered by a wire.
     * The list is ordered the same as the facingsHorizontal.
     * 
     * @param worldIn World
     * @param pos Position of the wire
     * @return List of all facings that can get powered by this wire
     */
    private List<EnumFacing> getSidesToPower(World worldIn, BlockPos pos) {
        List<EnumFacing> retval = Lists.<EnumFacing>newArrayList();
        for (EnumFacing facing : facingsHorizontal) {
            if (isPowerSourceAt(worldIn, pos, facing))
                retval.add(facing);
        }
        if (retval.isEmpty())
            return Lists.<EnumFacing>newArrayList(facingsHorizontal);
        boolean northsouth = retval.contains(EnumFacing.NORTH) || retval.contains(EnumFacing.SOUTH);
        boolean eastwest = retval.contains(EnumFacing.EAST) || retval.contains(EnumFacing.WEST);
        if (northsouth) {
            retval.remove(EnumFacing.EAST);
            retval.remove(EnumFacing.WEST);
        }
        if (eastwest) {
            retval.remove(EnumFacing.NORTH);
            retval.remove(EnumFacing.SOUTH);
        }
        return retval;
    }

    /**
     * Adds all surrounding positions to a set.
     * This is the neighbor blocks, as well as their neighbors 
     * 
     * @param pos
     * @param set
     */
    private void addAllSurroundingBlocks(BlockPos pos, Set<BlockPos> set) {
        for (Vec3i vect : surroundingBlocksOffset) {
            set.add(pos.add(vect));
        }
    }

    /**
     * Sets the block state of a wire with a new power level and marks for updates
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position at which the state needs to be set
     * @param state Old state
     * @param power Power it should get set to
     */
    private void setWireState(World worldIn, BlockPos pos, IBlockState state, int power) {
        state = state.withProperty(BlockRedstoneWire.POWER, Integer.valueOf(power));
        worldIn.setBlockState(pos, state, 2);
        updatedRedstoneWire.add(pos);
    }

    /**
     * @author panda
     * @reason Uses local surrounding block offset list for notifications.
     *
     * @param worldIn The world
     * @param pos The position
     * @param state The block state
     */
    @Overwrite
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            this.updateSurroundingRedstone(worldIn, pos);
            for (Vec3i vec : surroundingBlocksOffset) {
                worldIn.notifyNeighborsOfStateChange(pos.add(vec), this, false);
            }
        }
    }

    /**
     * @author panda
     * @reason Uses local surrounding block offset list for notifications.
     *
     * @param worldIn The world
     * @param pos The position
     */
    @Overwrite
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        if (!worldIn.isRemote) {
            this.updateSurroundingRedstone(worldIn, pos);
            for (Vec3i vec : surroundingBlocksOffset) {
                worldIn.notifyNeighborsOfStateChange(pos.add(vec), this, false);
            }
        }
    }

    /**
     * @author panda
     * @reason Changed to use getSidesToPower() to avoid duplicate implementation.
     *
     * @param blockState The block state
     * @param blockAccess The block access
     * @param pos The position
     * @param side The side
     */
    @Overwrite
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (!this.canProvidePower) {
            return 0;
        } else {
            if (side == EnumFacing.UP || this.getSidesToPower((World) blockAccess, pos).contains(side)) {
                return ((Integer) blockState.getValue(BlockRedstoneWire.POWER)).intValue();
            } else {
                return 0;
            }
        }
    }
}

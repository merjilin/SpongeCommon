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
package org.spongepowered.common.mixin.core.entity.item;

import net.minecraft.entity.item.EntityBoat;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.mutable.block.TreeData;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.mixin.core.entity.MixinEntity;

import java.util.List;

// TODO 1.9: Refactor this for boat overhaul
@Mixin(EntityBoat.class)
public abstract class MixinEntityBoat extends MixinEntity implements Boat {

//    @Shadow private double speedMultiplier;

    private double maxSpeed = 0.35D;
    private boolean moveOnLand = false;
    private double occupiedDecelerationSpeed = 0D;
    private double unoccupiedDecelerationSpeed = 0.8D;

    private double tempMotionX;
    private double tempMotionZ;
    private double tempSpeedMultiplier;
    private double initialDisplacement;

    // All of these injections need to be rewritten.
//    @Inject(method = "onUpdate()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityBoat;moveEntity(DDD)V"))
//    public void implementLandBoats(CallbackInfo ci) {
//        if (this.onGround && this.moveOnLand) {
//            this.motionX /= 0.5;
//            this.motionY /= 0.5;
//            this.motionZ /= 0.5;
//        }
//    }
//
//    @Inject(method = "onUpdate()V", at = @At(value = "INVOKE", target = "java.lang.Math.sqrt(D)D", ordinal = 0))
//    public void beforeModifyMotion(CallbackInfo ci) {
//        this.initialDisplacement = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
//    }
//
//    @Inject(method = "onUpdate()V", at = @At(value = "INVOKE", target = "java.lang.Math.sqrt(D)D", ordinal = 1))
//    public void beforeLimitSpeed(CallbackInfo ci) {
//        this.tempMotionX = this.motionX;
//        this.tempMotionZ = this.motionZ;
//        this.tempSpeedMultiplier = this.speedMultiplier;
//    }
//
//    @Inject(method = "onUpdate()V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/item/EntityBoat;onGround:Z", ordinal = 1))
//    public void afterLimitSpeed(CallbackInfo ci) {
//        this.motionX = this.tempMotionX;
//        this.motionZ = this.tempMotionZ;
//        this.speedMultiplier = this.tempSpeedMultiplier;
//        double displacement = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
//
//        if (displacement > this.maxSpeed) {
//            double ratio = this.maxSpeed / displacement;
//            this.motionX *= ratio;
//            this.motionZ *= ratio;
//            displacement = this.maxSpeed;
//        }
//
//        if ((displacement > this.initialDisplacement) && (this.speedMultiplier < this.maxSpeed)) {
//            this.speedMultiplier += (this.maxSpeed - this.speedMultiplier) / this.maxSpeed * 100.0;
//            this.speedMultiplier = Math.min(this.speedMultiplier, this.maxSpeed);
//        } else {
//            this.speedMultiplier -= (this.speedMultiplier - 0.07) / this.maxSpeed * 100.0;
//            this.speedMultiplier = Math.max(this.speedMultiplier, 0.07);
//        }
//    }

    // TODO: Re-enable this with support for multiple riding
    /*@Inject(method = "onUpdate()V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/item/EntityBoat;riddenByEntity:Lnet/minecraft/entity/Entity;", ordinal = 0))
    public void implementCustomDeceleration(CallbackInfo ci) {
        if (!(this.riddenByEntity instanceof EntityLivingBase)) {
            double decel = this.riddenByEntity == null ? this.unoccupiedDecelerationSpeed : this.occupiedDecelerationSpeed;
            this.motionX *= decel;
            this.motionZ *= decel;

            if (this.motionX < 0.00005) {
                this.motionX = 0.0;
            }

            if (this.motionZ < 0.00005) {
                this.motionZ = 0.0;
            }
        }
    }*/

    @Override
    public void supplyVanillaManipulators(List<DataManipulator<?, ?>> manipulators) {
        super.supplyVanillaManipulators(manipulators);
        manipulators.add(get(TreeData.class).get());
    }

    @Override
    public boolean isInWater() {
        return !this.onGround;
    }

    @Override
    public double getMaxSpeed() {
        return this.maxSpeed;
    }

    @Override
    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    @Override
    public boolean canMoveOnLand() {
        return this.moveOnLand;
    }

    @Override
    public void setMoveOnLand(boolean moveOnLand) {
        this.moveOnLand = moveOnLand;
    }

    @Override
    public double getOccupiedDeceleration() {
        return this.occupiedDecelerationSpeed;
    }

    @Override
    public void setOccupiedDeceleration(double occupiedDeceleration) {
        this.occupiedDecelerationSpeed = occupiedDeceleration;
    }

    @Override
    public double getUnoccupiedDeceleration() {
        return this.unoccupiedDecelerationSpeed;
    }

    @Override
    public void setUnoccupiedDeceleration(double unoccupiedDeceleration) {
        this.unoccupiedDecelerationSpeed = unoccupiedDeceleration;
    }

    @Override
    public void readFromNbt(NBTTagCompound compound) {
        super.readFromNbt(compound);
        if (compound.hasKey(NbtDataUtil.BOAT_MAX_SPEED)) {
            this.maxSpeed = compound.getDouble(NbtDataUtil.BOAT_MAX_SPEED);
        }
        if (compound.hasKey(NbtDataUtil.BOAT_MOVE_ON_LAND)) {
            this.moveOnLand = compound.getBoolean(NbtDataUtil.BOAT_MOVE_ON_LAND);
        }
        if (compound.hasKey(NbtDataUtil.BOAT_OCCUPIED_DECELERATION_SPEED)) {
            this.occupiedDecelerationSpeed = compound.getDouble(NbtDataUtil.BOAT_OCCUPIED_DECELERATION_SPEED);
        }
        if (compound.hasKey(NbtDataUtil.BOAT_UNOCCUPIED_DECELERATION_SPEED)) {
            this.unoccupiedDecelerationSpeed = compound.getDouble(NbtDataUtil.BOAT_UNOCCUPIED_DECELERATION_SPEED);
        }
    }

    @Override
    public void writeToNbt(NBTTagCompound compound) {
        super.writeToNbt(compound);
        compound.setDouble(NbtDataUtil.BOAT_MAX_SPEED, this.maxSpeed);
        compound.setBoolean(NbtDataUtil.BOAT_MOVE_ON_LAND, this.moveOnLand);
        compound.setDouble(NbtDataUtil.BOAT_OCCUPIED_DECELERATION_SPEED, this.occupiedDecelerationSpeed);
        compound.setDouble(NbtDataUtil.BOAT_UNOCCUPIED_DECELERATION_SPEED, this.unoccupiedDecelerationSpeed);
    }
}

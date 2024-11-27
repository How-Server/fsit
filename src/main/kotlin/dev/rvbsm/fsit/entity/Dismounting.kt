package dev.rvbsm.fsit.entity

import dev.rvbsm.fsit.util.math.toHorizontalDirection
import net.minecraft.entity.Dismounting.canDismountInBlock
import net.minecraft.entity.Dismounting.canPlaceEntityAt
import net.minecraft.entity.Dismounting.getDismountOffsets
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

/**
 * @see net.minecraft.entity.vehicle.AbstractMinecartEntity.updatePassengerForDismount
 * @see net.minecraft.entity.vehicle.BoatEntity.updatePassengerForDismount
 */
fun getDismountPosition(vehicle: Entity, passenger: LivingEntity): Vec3d {
    val world = vehicle.world
    val vehiclePos = vehicle.pos

    val dismountOffsets = getDismountOffsets(passenger.yaw.toHorizontalDirection())
    for ((xOffset, zOffset) in sequenceOf(intArrayOf(0, 0), *dismountOffsets)) {
        val dismountPos = vehiclePos.add(xOffset.toDouble(), 0.0, zOffset.toDouble()).let { dismountPos ->
            world.getDismountHeight(BlockPos.ofFloored(dismountPos)).takeIf(::canDismountInBlock)
                ?.let { dismountHeight -> dismountPos.add(0.0, dismountHeight, 0.0) }
        } ?: continue

        passenger.poses.find { canPlaceEntityAt(world, dismountPos, passenger, it) }?.let {
            passenger.pose = it
            return dismountPos
        }
    }

    return vehiclePos
}

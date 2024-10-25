package dev.rvbsm.fsit.client.networking

import dev.rvbsm.fsit.api.entity.PoseableEntity
import dev.rvbsm.fsit.entity.PlayerPose
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.Vec3d

fun ClientPlayerEntity.setPose(pose: PlayerPose, pos: Vec3d? = null) = (this as PoseableEntity).`fsit$setPose`(pose, pos)
fun ClientPlayerEntity.pose(): PlayerPose = (this as PoseableEntity).`fsit$getPose`()

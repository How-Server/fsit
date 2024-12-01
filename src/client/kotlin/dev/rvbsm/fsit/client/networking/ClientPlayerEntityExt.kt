package dev.rvbsm.fsit.client.networking

import dev.rvbsm.fsit.api.player.PlayerPose
import dev.rvbsm.fsit.entity.ModPose
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.Vec3d

fun ClientPlayerEntity.setPose(pose: ModPose, pos: Vec3d? = null) = (this as PlayerPose).`fsit$setPose`(pose, pos)
fun ClientPlayerEntity.pose(): ModPose = (this as PlayerPose).`fsit$getPose`()

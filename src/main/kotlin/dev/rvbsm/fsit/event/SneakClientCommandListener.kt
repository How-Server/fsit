package dev.rvbsm.fsit.event

import dev.rvbsm.fsit.api.event.ClientCommandCallback
import dev.rvbsm.fsit.entity.PlayerPose
import dev.rvbsm.fsit.entity.RideEntity
import dev.rvbsm.fsit.modTimeSource
import dev.rvbsm.fsit.networking.config
import dev.rvbsm.fsit.networking.setPose
import dev.rvbsm.fsit.util.math.toHorizontalDirection
import net.minecraft.entity.EntityPose
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark

private val sneaks = mutableMapOf<UUID, TimeMark>()

val ClientCommandSneakListener = ClientCommandCallback onClientCommand@{ player, mode ->
    if (mode == ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY && player.firstPassenger is RideEntity) {
        return@onClientCommand player.removeAllPassengers()
    }

    if (mode != ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY || !player.isOnGround) return@onClientCommand

    val config = player.config.onSneak.takeUnless { !it.sitting && !it.crawling } ?: return@onClientCommand
    if (player.pitch < config.minPitch) return@onClientCommand

    val sneakMark = sneaks.computeIfPresent(player.uuid) { _, mark ->
        mark.takeUnless { it.elapsedNow() > config.delay.milliseconds }
    }

    if (sneakMark == null) sneaks[player.uuid] = modTimeSource.markNow()
    else if (sneakMark.elapsedNow() <= config.delay.milliseconds) when {
        config.crawling && player.isNearGap() -> player.setPose(PlayerPose.Crawling)
        config.sitting -> player.setPose(PlayerPose.Sitting)
    }
}

private fun ServerPlayerEntity.isNearGap(): Boolean {
    val crawlingDimensions = this.getDimensions(EntityPose.SWIMMING)
    val crouchingDimensions = this.getDimensions(EntityPose.CROUCHING)

    val expectEmptyAt = pos.add(
        yaw.toHorizontalDirection().offsetX * 0.1,
        0.0,
        yaw.toHorizontalDirection().offsetZ * 0.1,
    )
    val expectFullAt = pos.add(
        yaw.toHorizontalDirection().offsetX * 0.1,
        crouchingDimensions.height.toDouble(),
        yaw.toHorizontalDirection().offsetZ * 0.1,
    )

    return world.isSpaceEmpty(this, crawlingDimensions.getBoxAt(expectEmptyAt).contract(1.0e-6)) &&
            !world.isSpaceEmpty(this, crawlingDimensions.getBoxAt(expectFullAt).contract(1.0e-6))
}

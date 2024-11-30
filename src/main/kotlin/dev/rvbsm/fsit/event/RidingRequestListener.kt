package dev.rvbsm.fsit.event

import dev.rvbsm.fsit.api.event.PassedUseEntityCallback
import dev.rvbsm.fsit.entity.RideEntity
import dev.rvbsm.fsit.modScope
import dev.rvbsm.fsit.networking.config
import dev.rvbsm.fsit.networking.payload.RidingRequestS2CPayload
import dev.rvbsm.fsit.networking.payload.RidingResponseC2SPayload
import dev.rvbsm.fsit.networking.trySend
import dev.rvbsm.fsit.util.xor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

private val requestTimeout = 5000.milliseconds
private val requests = mutableMapOf<UUID, Channel<Boolean>>()

internal val StartRidingListener = PassedUseEntityCallback interact@{ player, _, entity ->
    if (entity !is ServerPlayerEntity || !player.canStartRiding(entity)) return@interact ActionResult.PASS

    if (!player.config.onUse.riding || !entity.config.onUse.riding) return@interact ActionResult.PASS
    else if (!player.isInRange(entity, player.config.onUse.range.toDouble())) return@interact ActionResult.PASS

    requests.computeIfAbsent(player.uuid xor entity.uuid) { requestId ->
        Channel<Boolean>(capacity = 2, onBufferOverflow = BufferOverflow.DROP_LATEST).also {
            player.trySend(RidingRequestS2CPayload(entity.uuid)) { it.trySend(true) }
            entity.trySend(RidingRequestS2CPayload(player.uuid)) { it.trySend(true) }

            modScope.launch {
                withTimeout(requestTimeout) {
                    val response = it.receive() && it.receive()

                    if (response && player.canStartRiding(entity)) {
                        RideEntity.create(player, entity)
                    }
                }
            }.invokeOnCompletion { requests -= requestId }
        }
    }

    return@interact ActionResult.SUCCESS
}

internal fun RidingResponseC2SPayload.accept(player: ServerPlayerEntity) = modScope.launch {
    requests[player.uuid xor uuid]?.send(response.isAccepted)
}

private fun ServerPlayerEntity.shouldCancelRiding() = shouldCancelInteraction() || isSpectator || hasPassengers()

private fun ServerPlayerEntity.canStartRiding(other: ServerPlayerEntity) =
    this != other && uuid != other.uuid &&
            !shouldCancelRiding() && !other.shouldCancelRiding() &&
            config.onUse.riding && other.config.onUse.riding &&
            isInRange(other, config.onUse.range.toDouble())

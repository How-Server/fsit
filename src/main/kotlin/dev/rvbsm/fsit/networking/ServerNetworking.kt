package dev.rvbsm.fsit.networking

import dev.rvbsm.fsit.entity.RideEntity
import dev.rvbsm.fsit.event.completeRidingRequest
import dev.rvbsm.fsit.networking.payload.ConfigUpdateC2SPayload
import dev.rvbsm.fsit.networking.payload.CustomPayload
import dev.rvbsm.fsit.networking.payload.PoseRequestC2SPayload
import dev.rvbsm.fsit.networking.payload.RidingResponseC2SPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity

private val payloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

internal val ConfigUpdateC2SHandler = ServerPayloadHandler<ConfigUpdateC2SPayload> { player, _ ->
    payloadScope.launch { player.config = config }
}

internal val PoseRequestC2SHandler = ServerPayloadHandler<PoseRequestC2SPayload> {player, _ ->
    player.setPose(pose)
}

internal val RidingResponseC2SHandler = ServerPayloadHandler<RidingResponseC2SPayload> { player, _ ->
    if (!response.isAccepted && player.hasPassenger { (it as? RideEntity)?.isBelongsTo(uuid) == true }) {
        player.removeAllPassengers()
    }

    completeRidingRequest(player)
}

private typealias PlayPayloadHandler<P> =
        //? if <=1.20.4
        /*ServerPlayNetworking.PlayPacketHandler<P>*/
        //? if >=1.20.5
        ServerPlayNetworking.PlayPayloadHandler<P>

internal fun interface ServerPayloadHandler<P : CustomPayload<P>> : PlayPayloadHandler<P> {
    fun P.receive(player: ServerPlayerEntity, responseSender: PacketSender)

    //? if <=1.20.4 {
    /*override fun receive(packet: P, player: ServerPlayerEntity, responseSender: PacketSender) =
        packet.receive(player, responseSender)
    *///?} else if >=1.20.5 {
    override fun receive(payload: P, context: ServerPlayNetworking.Context) =
        payload.receive(context.player(), context.responseSender())
    //?}
}

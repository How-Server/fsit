package dev.rvbsm.fsit.client.networking

import dev.rvbsm.fsit.client.FSitModClient
import dev.rvbsm.fsit.client.event.untoggleKeyBindings
import dev.rvbsm.fsit.networking.payload.CustomPayload
import dev.rvbsm.fsit.networking.payload.PoseUpdateS2CPayload
import dev.rvbsm.fsit.networking.payload.RidingRequestS2CPayload
import dev.rvbsm.fsit.networking.payload.RidingResponseC2SPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.network.ClientPlayerEntity

internal val PoseUpdateS2CHandler = ClientPayloadHandler<PoseUpdateS2CPayload> { player, _ ->
    if (player.pose() != pose) {
        player.setPose(pose)
        untoggleKeyBindings()
    }
}

internal val RidingRequestS2CHandler = ClientPayloadHandler<RidingRequestS2CPayload> { _, responseSender ->
    responseSender.sendPacket(RidingResponseC2SPayload(playerUUID, !FSitModClient.isRestricted(playerUUID)))
}

private typealias PlayPayloadHandler<P> =
        //? if <=1.20.4
        ClientPlayNetworking.PlayPacketHandler<P>
        //? if >=1.20.5
        /*ClientPlayNetworking.PlayPayloadHandler<P>*/

internal fun interface ClientPayloadHandler<P : CustomPayload<P>> : PlayPayloadHandler<P> {
    fun P.handle(player: ClientPlayerEntity, responseSender: PacketSender)

    //? if <=1.20.4 {
    override fun receive(packet: P, player: ClientPlayerEntity, responseSender: PacketSender) =
        packet.handle(player, responseSender)
    //?} else if >=1.20.5 {
    /*override fun receive(payload: P, context: ClientPlayNetworking.Context) =
        payload.handle(context.player(), context.responseSender())
    *///?}
}

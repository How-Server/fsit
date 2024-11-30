package dev.rvbsm.fsit.networking.payload

import net.minecraft.network.NetworkSide
import net.minecraft.network.PacketByteBuf
import java.util.UUID

data class RidingRequestS2CPayload(val playerUUID: UUID) : CustomPayload<RidingRequestS2CPayload>(packetId) {
    override fun write(buf: PacketByteBuf) {
        buf.writeUuid(playerUUID)
    }

    companion object : Id<RidingRequestS2CPayload>("riding_request", NetworkSide.CLIENTBOUND) {
        override fun init(buf: PacketByteBuf) = RidingRequestS2CPayload(buf.readUuid())
    }
}

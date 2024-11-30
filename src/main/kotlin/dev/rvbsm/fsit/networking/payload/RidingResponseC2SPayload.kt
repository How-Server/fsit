package dev.rvbsm.fsit.networking.payload

import dev.rvbsm.fsit.networking.readEnumConstant
import net.minecraft.network.NetworkSide
import net.minecraft.network.PacketByteBuf
import java.util.UUID

data class RidingResponseC2SPayload(val uuid: UUID, val response: Response) :
    CustomPayload<RidingResponseC2SPayload>(packetId) {
    constructor(uuid: UUID, isAccepted: Boolean) : this(uuid, Response.valueOf(isAccepted))

    override fun write(buf: PacketByteBuf) {
        buf.writeUuid(uuid)
        buf.writeEnumConstant(response)
    }

    companion object : Id<RidingResponseC2SPayload>("riding_response", NetworkSide.SERVERBOUND) {
        override fun init(buf: PacketByteBuf) =
            RidingResponseC2SPayload(buf.readUuid(), buf.readEnumConstant<Response>())
    }

    enum class Response(val isAccepted: Boolean) {
        Accept(true), Refuse(false);

        companion object {
            fun valueOf(isAccepted: Boolean) = if (isAccepted) Accept else Refuse
        }
    }
}

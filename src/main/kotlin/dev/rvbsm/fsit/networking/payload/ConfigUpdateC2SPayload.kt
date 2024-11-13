package dev.rvbsm.fsit.networking.payload

import dev.rvbsm.fsit.config.ModConfig
import dev.rvbsm.fsit.config.serialization.ConfigSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import net.minecraft.network.NetworkSide
import net.minecraft.network.PacketByteBuf

@OptIn(ExperimentalSerializationApi::class)
private val jsonConfigSerializer =
    ConfigSerializer(format = Json { ignoreUnknownKeys = true; namingStrategy = JsonNamingStrategy.SnakeCase })

data class ConfigUpdateC2SPayload(val serializedConfig: String) : CustomPayload<ConfigUpdateC2SPayload>(packetId) {
    override fun write(buf: PacketByteBuf) {
        buf.writeString(serializedConfig)
    }

    suspend fun decode() = jsonConfigSerializer.decode(serializedConfig)

    companion object : Id<ConfigUpdateC2SPayload>("config_sync", NetworkSide.SERVERBOUND) {
        override fun init(buf: PacketByteBuf) = ConfigUpdateC2SPayload(buf.readString())

        suspend fun encode(config: ModConfig) = ConfigUpdateC2SPayload(jsonConfigSerializer.encode(config))
    }
}

package dev.rvbsm.fsit.config.serialization

import dev.rvbsm.fsit.modLogger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class DefaultedSerializer<T>(
    private val tSerializer: KSerializer<T>,
    private val defaultProvider: () -> T,
) : KSerializer<T> {

    override val descriptor = tSerializer.descriptor
    private val default get() = defaultProvider()

    override fun serialize(encoder: Encoder, value: T) {
        tSerializer.serialize(encoder, value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder) = runCatching {
        tSerializer.deserialize(decoder)
    }.onFailure {
        modLogger.error(
            "Failed to deserialize {}. Using the default default value",
            tSerializer.descriptor.capturedKClass?.qualifiedName, it,
        )
    }.getOrDefault(default)
}

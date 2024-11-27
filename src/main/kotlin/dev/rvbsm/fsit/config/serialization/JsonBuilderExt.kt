package dev.rvbsm.fsit.config.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.serializer

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> JsonBuilder.withDefault(noinline defaultProvider: () -> T) {
    serializersModule = serializersModule.overwriteWith(
        serializersModuleOf(
            DefaultedSerializer(
                serializersModule.getContextual(T::class) ?: serializersModule.serializer<T>(),
                defaultProvider,
            )
        )
    )
}

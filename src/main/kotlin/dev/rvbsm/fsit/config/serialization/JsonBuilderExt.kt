package dev.rvbsm.fsit.config.serialization

import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.serializersModuleOf

inline fun <reified T : Any> JsonBuilder.withDefault(noinline defaultProvider: () -> T) {
    serializersModule = serializersModule.overwriteWith(
        serializersModuleOf(DefaultedSerializer(serializersModule.preferContextual<T>(), defaultProvider))
    )
}

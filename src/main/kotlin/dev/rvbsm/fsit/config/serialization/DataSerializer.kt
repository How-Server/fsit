package dev.rvbsm.fsit.config.serialization

import dev.rvbsm.fsit.modLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

interface DataSerializer<SerialType> : SerialFormat {
    suspend fun <T> encodeToSerial(serializer: SerializationStrategy<T>, data: T): SerialType
    suspend fun <T> decodeFromSerial(deserializer: DeserializationStrategy<T>, serializedData: SerialType): T

    suspend fun <T> encode(serializer: SerializationStrategy<T>, data: T): Result<SerialType> =
        withContext(Dispatchers.IO) {
            runCatching {
                encodeToSerial(serializer, data)
            }.onFailure { modLogger.error("Failed to encode {}", data, it) }
        }

    suspend fun <T> decode(deserializer: DeserializationStrategy<T>, serializedData: SerialType): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                decodeFromSerial(deserializer, serializedData)
            }.onFailure { modLogger.error("Failed to decode {}", serializedData, it) }
        }
}

class StringDataSerializer<F : StringFormat>(format: F) : DataSerializer<String>, StringFormat by format {
    override suspend fun <T> encodeToSerial(serializer: SerializationStrategy<T>, data: T) =
        encodeToString(serializer, data)

    override suspend fun <T> decodeFromSerial(deserializer: DeserializationStrategy<T>, serializedData: String) =
        decodeFromString(deserializer, serializedData)
}

class BinaryDataSerializer<F : BinaryFormat>(format: F) : DataSerializer<ByteArray>, BinaryFormat by format {
    override suspend fun <T> encodeToSerial(serializer: SerializationStrategy<T>, data: T) =
        encodeToByteArray(serializer, data)

    override suspend fun <T> decodeFromSerial(deserializer: DeserializationStrategy<T>, serializedData: ByteArray) =
        decodeFromByteArray(deserializer, serializedData)
}

suspend inline fun <reified T : Any, S> DataSerializer<S>.encode(data: T): Result<S> =
    encode(serializersModule.preferContextual<T>(), data)

suspend inline fun <reified T : Any, S> DataSerializer<S>.decode(serializedData: S): Result<T> =
    decode(serializersModule.preferContextual<T>(), serializedData)

suspend inline fun <reified T : Any> StringDataSerializer<*>.encode(data: T): Result<String> = encode<T, String>(data)

suspend inline fun <reified T : Any> StringDataSerializer<*>.decode(serializedData: String): Result<T> =
    decode<T, String>(serializedData)

suspend inline fun <reified T : Any> BinaryDataSerializer<*>.encode(data: T): Result<ByteArray> =
    encode<T, ByteArray>(data)

suspend inline fun <reified T : Any> BinaryDataSerializer<*>.decode(serializedData: ByteArray): Result<T> =
    decode<T, ByteArray>(serializedData)

fun <F : StringFormat> F.asSerializer() = StringDataSerializer(this)
fun <F : BinaryFormat> F.asSerializer() = BinaryDataSerializer(this)

@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T : Any> SerializersModule.preferContextual() = getContextual(T::class) ?: serializer<T>()

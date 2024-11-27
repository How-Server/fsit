package dev.rvbsm.fsit.config.serialization

import dev.rvbsm.fsit.modLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.StringFormat
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isReadable
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

abstract class DataReader<Serial>(
    dataSerializer: DataSerializer<Serial>,
    private val directory: Path,
    private val id: String,
    private vararg val fileExtensions: String,
    val writeToFile: Boolean = false,
) : DataSerializer<Serial> by dataSerializer {

    private val defaultPath = directory.resolve("$id.${fileExtensions.first()}")

    @PublishedApi
    internal val configPath
        get() = directory.find { it.name == id && fileExtensions.any(it.extension::equals) } ?: defaultPath

    abstract fun Path.read(): Serial
    abstract fun Path.write(serializedData: Serial)

    @PublishedApi
    internal fun Path.canRead() = isReadable() && fileSize() > 0

    // todo: hm, this should not be generic at all
    suspend inline fun <reified T : Any> read(): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            decode<T, Serial>(configPath.read()).getOrThrow()
        }.onFailure { modLogger.error("Failed to read from {}", configPath, it) }.onSuccess { write(it) }
    }

    suspend inline fun <reified T : Any> write(data: T) = withContext(Dispatchers.IO) {
        if (writeToFile) runCatching {
            configPath.write(encode<T, Serial>(data).getOrThrow())
        }.onFailure { modLogger.error("Failed to write {} to {}", data, configPath, it) }
    }
}

class StringDataReader<F : StringFormat>(
    dataSerializer: StringDataSerializer<F>,
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean,
) : DataReader<String>(dataSerializer, directory, id, *fileExtensions, writeToFile = writeToFile) {

    override fun Path.read() = readText()
    override fun Path.write(serializedData: String) = writeText(serializedData)
}

class BinaryDataReader<F : BinaryFormat>(
    dataSerializer: BinaryDataSerializer<F>,
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean,
) : DataReader<ByteArray>(dataSerializer, directory, id, *fileExtensions, writeToFile = writeToFile) {

    override fun Path.read() = readBytes()
    override fun Path.write(serializedData: ByteArray) = writeBytes(serializedData)
}

fun <F : StringFormat> F.asReader(
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean = false,
) = StringDataReader(asSerializer(), directory, id, *fileExtensions, writeToFile = writeToFile)

fun <F : BinaryFormat> F.asReader(
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean = false,
) = BinaryDataReader(asSerializer(), directory, id, *fileExtensions, writeToFile = writeToFile)

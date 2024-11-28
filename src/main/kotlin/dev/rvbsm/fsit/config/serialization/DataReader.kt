package dev.rvbsm.fsit.config.serialization

import dev.rvbsm.fsit.modLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
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

abstract class DataReader<T : Any, Serial>(
    dataSerializer: DataSerializer<Serial>,
    private val tSerializer: KSerializer<T>,
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

    suspend fun read(): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            decode(tSerializer, configPath.read()).getOrThrow()
        }.onFailure { modLogger.error("Failed to read from {}", configPath, it) }.onSuccess { write(it) }
    }

    suspend fun write(data: T) = withContext(Dispatchers.IO) {
        if (writeToFile) runCatching {
            configPath.write(encode(tSerializer, data).getOrThrow())
        }.onFailure { modLogger.error("Failed to write {} to {}", data, configPath, it) }
    }
}

class StringDataReader<F : StringFormat, T : Any>(
    dataSerializer: StringDataSerializer<F>,
    tSerializer: KSerializer<T>,
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean,
) : DataReader<T, String>(dataSerializer, tSerializer, directory, id, *fileExtensions, writeToFile = writeToFile) {

    override fun Path.read() = readText()
    override fun Path.write(serializedData: String) = writeText(serializedData)
}

class BinaryDataReader<F : BinaryFormat, T : Any>(
    dataSerializer: BinaryDataSerializer<F>,
    tSerializer: KSerializer<T>,
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean,
) : DataReader<T, ByteArray>(dataSerializer, tSerializer, directory, id, *fileExtensions, writeToFile = writeToFile) {

    override fun Path.read() = readBytes()
    override fun Path.write(serializedData: ByteArray) = writeBytes(serializedData)
}

inline fun <F : StringFormat, reified T : Any> StringDataSerializer<F>.asReader(
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean = false,
) = StringDataReader(
    this, serializersModule.preferContextual<T>(),
    directory, id, *fileExtensions,
    writeToFile = writeToFile,
)

inline fun <F : BinaryFormat, reified T : Any> BinaryDataSerializer<F>.asReader(
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean = false,
) = BinaryDataReader(
    this, serializersModule.preferContextual<T>(),
    directory, id, *fileExtensions,
    writeToFile = writeToFile,
)

inline fun <F : StringFormat, reified T : Any> F.asReader(
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean = false,
) = asSerializer().asReader<F, T>(directory, id, *fileExtensions, writeToFile = writeToFile)

inline fun <F : BinaryFormat, reified T : Any> F.asReader(
    directory: Path, id: String, vararg fileExtensions: String,
    writeToFile: Boolean = false,
) = asSerializer().asReader<F, T>(directory, id, *fileExtensions, writeToFile = writeToFile)

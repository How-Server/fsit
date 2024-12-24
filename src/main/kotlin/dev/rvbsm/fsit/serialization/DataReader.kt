package dev.rvbsm.fsit.serialization

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
import kotlin.io.path.notExists
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

abstract class DataReader<T : Any, Serial>(
    dataSerializer: DataSerializer<Serial>,
    private val tSerializer: KSerializer<T>,
    private val directory: Path,
    private val id: String,
    private val fileExtensions: List<String>,
    private val writeToFile: Boolean = false,
    private val defaultProvider: () -> T,
) : DataSerializer<Serial> by dataSerializer {

    private val defaultPath = directory.resolve("$id.${fileExtensions.first()}")
    private val defaultData get() = defaultProvider()

    @PublishedApi
    internal val dataPath
        get() = directory.find { it.name == id && fileExtensions.any(it.extension::equals) } ?: defaultPath

    abstract fun Path.read(): Serial
    abstract fun Path.write(serializedData: Serial)

    @PublishedApi
    internal fun Path.canRead() = isReadable() && fileSize() > 0

    suspend fun read(): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            if (dataPath.notExists() || !dataPath.canRead()) defaultData
            else decode(tSerializer, dataPath.read()).getOrThrow()
        }.onFailure { modLogger.error("Failed to read from {}", dataPath, it) }.onSuccess { write(it) }
    }

    suspend fun write(data: T) = withContext(Dispatchers.IO) {
        if (writeToFile) runCatching {
            dataPath.write(encode(tSerializer, data).getOrThrow())
        }.onFailure { modLogger.error("Failed to write {} to {}", data, dataPath, it) }
    }
}

class StringDataReader<F : StringFormat, T : Any>(
    dataSerializer: StringDataSerializer<F>,
    tSerializer: KSerializer<T>,
    directory: Path, id: String, fileExtensions: List<String>,
    writeToFile: Boolean,
    defaultProvider: () -> T,
) : DataReader<T, String>(dataSerializer, tSerializer, directory, id, fileExtensions, writeToFile, defaultProvider) {

    override fun Path.read() = readText()
    override fun Path.write(serializedData: String) = writeText(serializedData)
}

class BinaryDataReader<F : BinaryFormat, T : Any>(
    dataSerializer: BinaryDataSerializer<F>,
    tSerializer: KSerializer<T>,
    directory: Path, id: String, fileExtensions: List<String>,
    writeToFile: Boolean,
    defaultProvider: () -> T,
) : DataReader<T, ByteArray>(dataSerializer, tSerializer, directory, id, fileExtensions, writeToFile, defaultProvider) {

    override fun Path.read() = readBytes()
    override fun Path.write(serializedData: ByteArray) = writeBytes(serializedData)
}

inline fun <F : StringFormat, reified T : Any> StringDataSerializer<F>.asReader(
    directory: Path, id: String, fileExtensions: List<String>,
    writeToFile: Boolean = false,
    noinline defaultProvider: () -> T,
) = StringDataReader(
    this, serializersModule.preferContextual<T>(),
    directory, id, fileExtensions, writeToFile,
    defaultProvider,
)

inline fun <F : BinaryFormat, reified T : Any> BinaryDataSerializer<F>.asReader(
    directory: Path, id: String, fileExtensions: List<String>,
    writeToFile: Boolean = false,
    noinline defaultProvider: () -> T,
) = BinaryDataReader(
    this, serializersModule.preferContextual<T>(),
    directory, id, fileExtensions, writeToFile,
    defaultProvider,
)

inline fun <F : StringFormat, reified T : Any> StringDataSerializer<F>.asReader(
    directory: Path, id: String, fileExtension: String,
    writeToFile: Boolean = false,
    noinline defaultProvider: () -> T,
) = asReader(directory, id, listOf(fileExtension), writeToFile, defaultProvider)

inline fun <F : BinaryFormat, reified T : Any> BinaryDataSerializer<F>.asReader(
    directory: Path, id: String, fileExtension: String,
    writeToFile: Boolean = false,
    noinline defaultProvider: () -> T,
) = asReader(directory, id, listOf(fileExtension), writeToFile, defaultProvider)

inline fun <F : StringFormat, reified T : Any> F.asReader(
    directory: Path, id: String, fileExtensions: List<String>,
    writeToFile: Boolean = false,
    noinline defaultProvider: () -> T,
) = asSerializer().asReader<F, T>(directory, id, fileExtensions, writeToFile, defaultProvider)

inline fun <F : BinaryFormat, reified T : Any> F.asReader(
    directory: Path, id: String, fileExtensions: List<String>,
    writeToFile: Boolean = false,
    noinline defaultProvider: () -> T,
) = asSerializer().asReader<F, T>(directory, id, fileExtensions, writeToFile, defaultProvider)

inline fun <F : StringFormat, reified T : Any> F.asReader(
    directory: Path, id: String, fileExtension: String,
    writeToFile: Boolean = false,
    noinline defaultProvider: () -> T,
) = asReader(directory, id, listOf(fileExtension), writeToFile, defaultProvider)

inline fun <F : BinaryFormat, reified T : Any> F.asReader(
    directory: Path, id: String, fileExtension: String,
    writeToFile: Boolean = false,
    noinline defaultProvider: () -> T,
) = asReader(directory, id, listOf(fileExtension), writeToFile, defaultProvider)

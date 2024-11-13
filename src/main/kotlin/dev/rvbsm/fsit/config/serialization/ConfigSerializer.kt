package dev.rvbsm.fsit.config.serialization

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.yamlMap
import dev.rvbsm.fsit.config.ModConfig
import dev.rvbsm.fsit.config.migrations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

internal val configDirPath = FabricLoader.getInstance().configDir

internal open class ConfigSerializer(private val format: StringFormat) : StringFormat by format {
    suspend fun encode(config: ModConfig): String = withContext(Dispatchers.IO) {
        encodeToString(config)
    }

    suspend fun decode(string: String): ModConfig = withContext(Dispatchers.IO) {
        runCatching {
            when (format) {
                is Json -> format.decodeWithMigrations(string)
                is Yaml -> format.decodeWithMigrations(string)

                else -> decodeFromString<ModConfig>(string)
            }
        }.getOrNull() ?: ModConfig()
    }

    private fun Json.decodeWithMigrations(string: String): ModConfig =
        decodeFromJsonElement(parseToJsonElement(string).jsonObject.let { it.migrate(it.migrations) })

    private fun Yaml.decodeWithMigrations(string: String): ModConfig =
        decodeFromYamlNode(parseToYamlNode(string).yamlMap.let { it.migrate(it.migrations) })

    internal class Writable(
        format: StringFormat,
        private val id: String,
        private vararg val fileExtensions: String,
    ) : ConfigSerializer(format) {

        private val defaultPath = configDirPath.resolve("$id.${fileExtensions.first()}")
        private val configPath =
            configDirPath.find { it.name == id && fileExtensions.any(it.extension::equals) } ?: defaultPath

        internal suspend fun write(config: ModConfig) {
            withContext(Dispatchers.IO) {
                config.path?.writeText(encode(config))
            }
        }

        private suspend fun readOrDefault(path: Path) =
            if (path.exists() && path.fileSize() > 0) decode(path.readText())
            else ModConfig()

        internal suspend fun read() = withContext(Dispatchers.IO) {
            readOrDefault(configPath).copy(path = defaultPath).also { write(it) }
        }
    }
}

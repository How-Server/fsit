package dev.rvbsm.fsit

import com.charleskorn.kaml.YamlNamingStrategy
import dev.rvbsm.fsit.api.event.ClientCommandCallback
import dev.rvbsm.fsit.api.event.PassedUseBlockCallback
import dev.rvbsm.fsit.api.event.PassedUseEntityCallback
import dev.rvbsm.fsit.api.event.UpdatePoseCallback
import dev.rvbsm.fsit.command.CommandBuilder
import dev.rvbsm.fsit.command.command
import dev.rvbsm.fsit.command.isGameMaster
import dev.rvbsm.fsit.config.ModConfig
import dev.rvbsm.fsit.config.configSchemas
import dev.rvbsm.fsit.config.getOrDefault
import dev.rvbsm.fsit.config.migration.version
import dev.rvbsm.fsit.config.migration.withMigration
import dev.rvbsm.fsit.config.serialization.UUIDSerializer
import dev.rvbsm.fsit.config.serialization.Yaml
import dev.rvbsm.fsit.config.serialization.asReader
import dev.rvbsm.fsit.config.serialization.asSerializer
import dev.rvbsm.fsit.config.serialization.withDefault
import dev.rvbsm.fsit.entity.PlayerPose
import dev.rvbsm.fsit.event.ClientCommandSneakListener
import dev.rvbsm.fsit.event.SpawnSeatListener
import dev.rvbsm.fsit.event.StartRidingListener
import dev.rvbsm.fsit.event.UpdatePoseListener
import dev.rvbsm.fsit.networking.ConfigUpdateC2SHandler
import dev.rvbsm.fsit.networking.PoseRequestC2SHandler
import dev.rvbsm.fsit.networking.RidingResponseC2SHandler
import dev.rvbsm.fsit.networking.isInPose
import dev.rvbsm.fsit.networking.payload.ConfigUpdateC2SPayload
import dev.rvbsm.fsit.networking.payload.PoseRequestC2SPayload
import dev.rvbsm.fsit.networking.payload.RidingResponseC2SPayload
import dev.rvbsm.fsit.networking.resetPose
import dev.rvbsm.fsit.networking.setPose
import dev.rvbsm.fsit.util.id
import dev.rvbsm.fsit.util.text.literal
import dev.rvbsm.fsit.util.text.translatable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.serializersModuleOf
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.command.ServerCommandSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KMutableProperty0
import kotlin.time.TimeSource

@PublishedApi
internal val modLogger: Logger = LoggerFactory.getLogger(FSitMod::class.java)

@PublishedApi
internal lateinit var modScope: CoroutineScope
    private set

val modTimeSource = TimeSource.Monotonic

@OptIn(ExperimentalSerializationApi::class)
@Suppress("json_format_redundant")
val jsonSerializer = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase

    serializersModule += serializersModuleOf(UUIDSerializer)

    withMigration<ModConfig>(configSchemas) { if ("config_version" in this) -1 else version }
    withDefault(::ModConfig)
}.asSerializer()

val yamlSerializer = Yaml {
    strictMode = false
    yamlNamingStrategy = YamlNamingStrategy.SnakeCase

    serializersModule += serializersModuleOf(UUIDSerializer)

    withMigration<ModConfig>(configSchemas)
    withDefault(::ModConfig)
}.asSerializer()

object FSitMod : ModInitializer {
    const val MOD_ID = "fsit"

    private val configReader =
        yamlSerializer.asReader(FabricLoader.getInstance().configDir, MOD_ID, "yml", "yaml", writeToFile = true)

    @JvmStatic
    lateinit var config: ModConfig private set

    @JvmStatic
    fun id(path: String) = path.id(MOD_ID)

    @JvmStatic
    fun translatable(category: String, path: String, vararg args: Any) = "$category.$MOD_ID.$path".translatable(args)

    private suspend fun loadConfig() {
        config = configReader.read<ModConfig>().getOrDefault()
    }

    suspend fun saveConfig() {
        configReader.write(config)
    }

    override fun onInitialize() {
        runBlocking { loadConfig() }

        registerPayloads()
        registerEvents()
        registerCommands()
    }

    private fun registerPayloads() {
        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateC2SPayload.packetId, ConfigUpdateC2SHandler)
        ServerPlayNetworking.registerGlobalReceiver(PoseRequestC2SPayload.packetId, PoseRequestC2SHandler)
        ServerPlayNetworking.registerGlobalReceiver(RidingResponseC2SPayload.packetId, RidingResponseC2SHandler)
    }

    private fun registerEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register {
            modScope = CoroutineScope(it.asCoroutineDispatcher() + SupervisorJob())
        }

        PassedUseEntityCallback.EVENT.register(StartRidingListener)
        PassedUseBlockCallback.EVENT.register(SpawnSeatListener)
        ClientCommandCallback.EVENT.register(ClientCommandSneakListener)
        UpdatePoseCallback.EVENT.register(UpdatePoseListener)
    }

    private fun registerCommands() {
        command(MOD_ID) {
            requires(ServerCommandSource::isGameMaster)

            literal("reload") executesSuspend {
                loadConfig()
                source.sendFeedback("Reloaded config!"::literal, true)
            }

            configArgument("useServer") { config::useServer }
            configArgument("centerSeats") { config.sitting::shouldCenter }
            configArgument("onUseSit") { config.onUse::sitting }
            configArgument("onUseRide") { config.onUse::riding }
            configArgument("onUseRange") { config.onUse::range }
            configArgument("onUseCheckSuffocation") { config.onUse::checkSuffocation }
            configArgument("onSneakSit") { config.onSneak::sitting }
            configArgument("onSneakCrawl") { config.onSneak::crawling }
            configArgument("onSneakMinPitch") { config.onSneak::minPitch }
            configArgument("onSneakDelay") { config.onSneak::delay }
        }

        fun poseCommand(name: String, pose: PlayerPose) = command(name) {
            requires(ServerCommandSource::isExecutedByPlayer)

            executes {
                val player = source.player!!
                if (player.hasVehicle()) return@executes

                if (player.isInPose()) player.resetPose()
                else player.setPose(pose)
            }
        }

        poseCommand("sit", PlayerPose.Sitting)
        poseCommand("crawl", PlayerPose.Crawling)
    }
}

private inline fun <reified T> CommandBuilder<ServerCommandSource, *>.configArgument(
    name: String,
    crossinline propertyGetter: () -> KMutableProperty0<T>,
) = literal(name) {
    executes {
        val property = propertyGetter()
        source.sendFeedback("Config option $name is currently set to: ${property.get()}"::literal, false)
    }

    argument<T>("value") {
        executesSuspend {
            val property = propertyGetter()
            property.set(it()).also { FSitMod.saveConfig() }
            source.sendFeedback("Config option $name is now set to: ${property.get()}"::literal, true)
        }
    }
}

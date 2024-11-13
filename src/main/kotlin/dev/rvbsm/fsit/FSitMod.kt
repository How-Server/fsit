package dev.rvbsm.fsit

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import dev.rvbsm.fsit.api.event.ClientCommandCallback
import dev.rvbsm.fsit.api.event.PassedUseBlockCallback
import dev.rvbsm.fsit.api.event.PassedUseEntityCallback
import dev.rvbsm.fsit.api.event.UpdatePoseCallback
import dev.rvbsm.fsit.command.CommandBuilder
import dev.rvbsm.fsit.command.command
import dev.rvbsm.fsit.command.isGameMaster
import dev.rvbsm.fsit.config.ModConfig
import dev.rvbsm.fsit.config.serialization.ConfigSerializer
import dev.rvbsm.fsit.entity.PlayerPose
import dev.rvbsm.fsit.event.ClientCommandSneakListener
import dev.rvbsm.fsit.event.ServerStoppingListener
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.command.ServerCommandSource
import kotlin.reflect.KMutableProperty0
import kotlin.time.TimeSource

val modTimeSource = TimeSource.Monotonic

object FSitMod : ModInitializer {
    const val MOD_ID = "fsit"

    @JvmStatic
    lateinit var config: ModConfig private set

    private val yamlConfigSerializer = ConfigSerializer.Writable(
        format = Yaml(
            configuration = YamlConfiguration(
                strictMode = false,
                yamlNamingStrategy = YamlNamingStrategy.SnakeCase,
            ),
        ), id = MOD_ID, "yml", "yaml"
    )

    override fun onInitialize() = runBlocking {
        loadConfig()

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
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerStoppingListener)

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

    @JvmStatic
    fun id(path: String) = path.id(MOD_ID)

    @JvmStatic
    fun translatable(category: String, path: String, vararg args: Any) = "$category.$MOD_ID.$path".translatable(args)

    private suspend fun loadConfig() = coroutineScope { config = yamlConfigSerializer.read() }
    fun saveConfig() = yamlConfigSerializer.write(config)
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
        executes {
            val property = propertyGetter()
            property.set(it()).also { FSitMod.saveConfig() }
            source.sendFeedback("Config option $name is now set to: ${property.get()}"::literal, true)
        }
    }
}

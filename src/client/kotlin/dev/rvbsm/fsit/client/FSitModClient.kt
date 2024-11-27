package dev.rvbsm.fsit.client

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.rvbsm.fsit.FSitMod
import dev.rvbsm.fsit.FSitMod.MOD_ID
import dev.rvbsm.fsit.client.command.command
import dev.rvbsm.fsit.client.event.KeyBindingsListener
import dev.rvbsm.fsit.client.event.poseKeyBindings
import dev.rvbsm.fsit.client.networking.PoseUpdateS2CHandler
import dev.rvbsm.fsit.client.networking.RidingRequestS2CHandler
import dev.rvbsm.fsit.client.option.KeyBindingMode
import dev.rvbsm.fsit.client.option.enumOption
import dev.rvbsm.fsit.config.serialization.asReader
import dev.rvbsm.fsit.jsonSerializer
import dev.rvbsm.fsit.networking.payload.ConfigUpdateC2SPayload
import dev.rvbsm.fsit.networking.payload.CustomPayload
import dev.rvbsm.fsit.networking.payload.PoseUpdateS2CPayload
import dev.rvbsm.fsit.networking.payload.RidingRequestS2CPayload
import dev.rvbsm.fsit.networking.payload.RidingResponseC2SPayload
import dev.rvbsm.fsit.util.text.literal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.text.Text
import java.util.UUID

private typealias RestrictionSet = MutableSet<UUID>

internal lateinit var modClientScope: CoroutineScope
    private set

private val alreadyRestrictedException =
    SimpleCommandExceptionType("Nothing changed. The player is already restricted".literal())
private val alreadyAllowedException =
    SimpleCommandExceptionType("Nothing changed. The player isn't restricted".literal())

private val restrictionsReader =
    jsonSerializer.asReader(FabricLoader.getInstance().configDir, "$MOD_ID.restrictions", "json", writeToFile = true)

object FSitModClient : ClientModInitializer {

    @JvmStatic
    val isServerFSitCompatible get() = ClientPlayNetworking.canSend(ConfigUpdateC2SPayload.packetId)

    @JvmStatic
    val sitMode = enumOption("key.fsit.sit", KeyBindingMode.Hybrid)

    @JvmStatic
    val crawlMode = enumOption("key.fsit.crawl", KeyBindingMode.Hybrid)

    private lateinit var socialRestrictions: RestrictionSet

    private fun RestrictionSet.store(uuid: UUID) = add(uuid).also { modClientScope.launch { saveRestrictions() } }
    private fun RestrictionSet.delete(uuid: UUID) = remove(uuid).also { modClientScope.launch { saveRestrictions() } }

    @JvmStatic
    fun restrictInteractionsFor(uuid: UUID) = socialRestrictions.store(uuid).also removeRestrictedPassenger@{
        val player = MinecraftClient.getInstance().player ?: return@removeRestrictedPassenger
        if (player.hasPassenger { it.uuid == uuid }) {
            trySend(RidingResponseC2SPayload(uuid, false)) // todo: is there any vanilla way?
        }
    }

    @JvmStatic
    fun allowInteractionsFor(uuid: UUID) = socialRestrictions.delete(uuid)

    @JvmStatic
    fun isRestricted(uuid: UUID) = uuid in socialRestrictions

    fun restrictOrThrow(uuid: UUID) = restrictInteractionsFor(uuid).also {
        if (!it) throw alreadyRestrictedException.create()
    }

    fun allowOrThrow(uuid: UUID) = allowInteractionsFor(uuid).also {
        if (!it) throw alreadyAllowedException.create()
    }

    internal fun <T> trySend(payload: T, orAction: () -> Unit = {}) where T : CustomPayload<T> {
        if (ClientPlayNetworking.canSend(payload.id)) {
            ClientPlayNetworking.send(payload)
        } else orAction()
    }

    private suspend fun loadRestrictions() {
        socialRestrictions = restrictionsReader.read<RestrictionSet>().getOrDefault(mutableSetOf())
    }

    private suspend fun saveRestrictions() {
        restrictionsReader.write(socialRestrictions)
    }

    internal suspend fun saveConfig() {
        FSitMod.saveConfig()
        syncConfig()
    }

    private suspend fun syncConfig() {
        trySend(ConfigUpdateC2SPayload.encode(FSitMod.config))
    }

    override fun onInitializeClient() = runBlocking {
        loadRestrictions()

        registerClientPayloads()
        registerClientEvents()
        registerClientCommands()
        registerKeyBindings()
    }

    private fun registerClientPayloads() {
        ClientPlayNetworking.registerGlobalReceiver(PoseUpdateS2CPayload.packetId, PoseUpdateS2CHandler)
        ClientPlayNetworking.registerGlobalReceiver(RidingRequestS2CPayload.packetId, RidingRequestS2CHandler)
    }

    private fun registerClientEvents() {
        ClientLifecycleEvents.CLIENT_STARTED.register {
            modClientScope = CoroutineScope(it.asCoroutineDispatcher() + SupervisorJob())
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            modClientScope.launch { syncConfig() }
        }
    }

    private fun registerClientCommands() {
        command("$MOD_ID:client") {
            fun restrictionCommand(name: String, action: (UUID) -> Boolean, message: (Text?) -> Text) = literal(name) {
                argument("player", { source.playerNames }) { playerName ->
                    executes {
                        val entry = source.client.networkHandler?.getPlayerListEntry(playerName())
                            ?: throw GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION.create()

                        if (action(entry.profile.id)) {
                            source.sendFeedback(message(entry.displayName))
                        }
                    }
                }
            }

            restrictionCommand("allow", ::allowOrThrow) { "Successfully allowed $it.".literal() }
            restrictionCommand("restrict", ::restrictOrThrow) { "Successfully restricted $it.".literal() }
        }
    }

    private fun registerKeyBindings() {
        poseKeyBindings.forEach { KeyBindingHelper.registerKeyBinding(it) }
        ClientTickEvents.END_CLIENT_TICK.register(KeyBindingsListener)
    }
}

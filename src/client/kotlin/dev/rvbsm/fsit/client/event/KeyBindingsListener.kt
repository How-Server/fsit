package dev.rvbsm.fsit.client.event

import dev.rvbsm.fsit.FSitMod
import dev.rvbsm.fsit.client.FSitModClient
import dev.rvbsm.fsit.client.networking.pose
import dev.rvbsm.fsit.client.networking.setPose
import dev.rvbsm.fsit.client.option.HybridKeyBinding
import dev.rvbsm.fsit.entity.ModPose
import dev.rvbsm.fsit.networking.payload.PoseRequestC2SPayload
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.option.KeyBinding
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.milliseconds

private val sitKey = HybridKeyBinding(
    "key.fsit.sit",
    GLFW.GLFW_KEY_RIGHT_CONTROL,
    KeyBinding.MISC_CATEGORY,
    500.milliseconds,
    FSitModClient.sitMode::getValue,
)

private val crawlKey = HybridKeyBinding(
    "key.fsit.crawl",
    GLFW.GLFW_KEY_RIGHT_ALT,
    KeyBinding.MISC_CATEGORY,
    500.milliseconds,
    FSitModClient.crawlMode::getValue,
)

val poseKeyBindings = arrayOf(sitKey, crawlKey)
private var wasPressed = false

val KeyBindingsListener = ClientTickEvents.EndTick tick@{ client ->
    if (!FSitModClient.isServerFSitCompatible) return@tick

    val player = client.player ?: return@tick
    if (!FSitMod.config.sitting.behaviour.shouldMove && !player.isOnGround && !player.hasVehicle()) return@tick

    val pose = when (player.pose()) {
        ModPose.Standing -> when {
            sitKey.isPressed -> ModPose.Sitting
            crawlKey.isPressed -> ModPose.Crawling
            else -> return@tick
        }

        ModPose.Sitting -> if (!sitKey.isPressed && wasPressed) ModPose.Standing else return@tick
        ModPose.Crawling -> if (!crawlKey.isPressed && wasPressed) ModPose.Standing else return@tick

        else -> return@tick
    }

    wasPressed = pose != ModPose.Standing
    if (!wasPressed) {
        untoggleKeyBindings()
    }

    player.setPose(pose)
    FSitModClient.trySend(PoseRequestC2SPayload(pose))
}

internal fun untoggleKeyBindings() {
    poseKeyBindings.forEach { it.untoggle() }
}

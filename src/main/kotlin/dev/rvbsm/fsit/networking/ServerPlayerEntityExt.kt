package dev.rvbsm.fsit.networking

import dev.rvbsm.fsit.api.network.RidingRequestHandler
import dev.rvbsm.fsit.api.network.ServerPlayerVelocity
import dev.rvbsm.fsit.api.player.PlayerConfig
import dev.rvbsm.fsit.api.player.PlayerCrawl
import dev.rvbsm.fsit.api.player.PlayerPose
import dev.rvbsm.fsit.config.ModConfig
import dev.rvbsm.fsit.entity.CrawlEntity
import dev.rvbsm.fsit.entity.ModPose
import dev.rvbsm.fsit.networking.payload.CustomPayload
import dev.rvbsm.fsit.networking.payload.RidingResponseC2SPayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal fun <P> ServerPlayerEntity.trySend(payload: P, orAction: () -> Unit = {}) where P : CustomPayload<P> {
    if (ServerPlayNetworking.canSend(this, payload.id)) {
        ServerPlayNetworking.send(this, payload)
    } else orAction()
}

fun ServerPlayerEntity.setPose(pose: ModPose, pos: Vec3d? = null) = (this as PlayerPose).`fsit$setPose`(pose, pos)
fun ServerPlayerEntity.resetPose() = (this as PlayerPose).`fsit$resetPose`()
fun ServerPlayerEntity.isInPose() = (this as PlayerPose).`fsit$isInPose`()

fun ServerPlayerEntity.setCrawl(crawlEntity: CrawlEntity) = (this as PlayerCrawl).`fsit$startCrawling`(crawlEntity)
fun ServerPlayerEntity.removeCrawl() = (this as PlayerCrawl).`fsit$stopCrawling`()
fun ServerPlayerEntity.hasCrawl() = (this as PlayerCrawl).`fsit$isCrawling`()

var ServerPlayerEntity.config: ModConfig
    get() = (this as PlayerConfig).`fsit$getConfig`()
    set(config) = (this as PlayerConfig).`fsit$setConfig`(config)

fun ServerPlayerEntity.hasConfig() = (this as PlayerConfig).`fsit$hasConfig`()

val ServerPlayerEntity.realVelocity get() = (this as ServerPlayerVelocity).`fsit$getPlayerVelocity`()

fun ServerPlayerEntity.sendRidingRequest(playerUUID: UUID, timeout: Duration) =
    (networkHandler as RidingRequestHandler).`fsit$sendRidingRequest`(playerUUID, timeout.toJavaDuration())

fun ServerPlayerEntity.onRidingResponse(response: RidingResponseC2SPayload) =
    (networkHandler as RidingRequestHandler).`fsit$onRidingResponse`(response)

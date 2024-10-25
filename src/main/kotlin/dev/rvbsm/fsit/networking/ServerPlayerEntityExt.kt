package dev.rvbsm.fsit.networking

import dev.rvbsm.fsit.api.entity.ConfigurableEntity
import dev.rvbsm.fsit.api.entity.CrawlableEntity
import dev.rvbsm.fsit.api.entity.PoseableEntity
import dev.rvbsm.fsit.api.network.ServerPlayerVelocity
import dev.rvbsm.fsit.entity.CrawlEntity
import dev.rvbsm.fsit.entity.PlayerPose
import dev.rvbsm.fsit.networking.payload.CustomPayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

internal fun <T> ServerPlayerEntity.trySend(payload: T, orAction: () -> Unit = {}) where T : CustomPayload<T> {
    if (ServerPlayNetworking.canSend(this, payload.id)) {
        ServerPlayNetworking.send(this, payload)
    } else orAction()
}

fun ServerPlayerEntity.setPose(pose: PlayerPose, pos: Vec3d? = null) = (this as PoseableEntity).`fsit$setPose`(pose, pos)
fun ServerPlayerEntity.resetPose() = (this as PoseableEntity).`fsit$resetPose`()
fun ServerPlayerEntity.isInPose() = (this as PoseableEntity).`fsit$isInPose`()

fun ServerPlayerEntity.setCrawl(crawlEntity: CrawlEntity) = (this as CrawlableEntity).`fsit$startCrawling`(crawlEntity)
fun ServerPlayerEntity.removeCrawl() = (this as CrawlableEntity).`fsit$stopCrawling`()
fun ServerPlayerEntity.hasCrawl() = (this as CrawlableEntity).`fsit$isCrawling`()

var ServerPlayerEntity.config
    get() = (this as ConfigurableEntity).`fsit$getConfig`()
    set(config) = (this as ConfigurableEntity).`fsit$setConfig`(config)
fun ServerPlayerEntity.hasConfig() = (this as ConfigurableEntity).`fsit$hasConfig`()

val ServerPlayerEntity.realVelocity get() = (this as ServerPlayerVelocity).`fsit$getPlayerVelocity`()

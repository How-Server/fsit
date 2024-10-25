package dev.rvbsm.fsit.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.rvbsm.fsit.FSitMod;
import dev.rvbsm.fsit.api.entity.ConfigurableEntity;
import dev.rvbsm.fsit.api.entity.CrawlableEntity;
import dev.rvbsm.fsit.api.event.UpdatePoseCallback;
import dev.rvbsm.fsit.api.network.ServerPlayerVelocity;
import dev.rvbsm.fsit.config.ModConfig;
import dev.rvbsm.fsit.entity.CrawlEntity;
import dev.rvbsm.fsit.entity.DismountingKt;
import dev.rvbsm.fsit.entity.PlayerPose;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntityMixin implements ConfigurableEntity, CrawlableEntity, ServerPlayerVelocity {
    @Shadow
    public abstract void stopRiding();

    @Unique
    private @Nullable ModConfig config;
    @Unique
    private @Nullable CrawlEntity crawlEntity;
    @Unique
    private @NotNull Vec3d playerVelocity = Vec3d.ZERO;

    @Inject(method = "playerTick", at = @At("TAIL"))
    private void tickPosing(CallbackInfo ci) {
        if (this.fsit$isInPose()) {
            if (this.getAbilities().flying || this.isSneaking()) {
                this.fsit$resetPose();
            }

            if (this.crawlEntity != null) {
                this.crawlEntity.tick();
            }
        }
    }

    @Inject(method = "onDisconnect", at = @At("TAIL"))
    private void dismountSeat(CallbackInfo ci) {
        if (this.fsit$isInPose(PlayerPose.Sitting)) {
            this.stopRiding();
        }
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void copyConfig(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        final ConfigurableEntity configurablePlayer = (ConfigurableEntity) oldPlayer;
        if (configurablePlayer.fsit$hasConfig()) {
            this.config = configurablePlayer.fsit$getConfig();
        }
    }

    @Inject(method = "stopRiding", at = @At("TAIL"))
    private void resetPose(CallbackInfo ci, @Local Entity entity) {
        if (this.fsit$isInPose(PlayerPose.Sitting)) {
            this.fsit$resetPose();
        }
    }

    @Override
    protected void move(MovementType movementType, Vec3d movement, CallbackInfo ci, Vec3d velocity) {
        this.playerVelocity = velocity;
    }

    @Override
    public boolean hasPlayerRider(boolean original) {
        return false;
    }

    @Override
    public Vec3d updatePassengerForDismount(Vec3d original, LivingEntity passenger) {
        return DismountingKt.getDismountPosition((ServerPlayerEntity) (Object) this, passenger);
    }

    @Override
    public void fsit$setConfig(@NotNull ModConfig config) {
        this.config = config;
    }

    @Override
    public @NotNull ModConfig fsit$getConfig() {
        if (FSitMod.getConfig().getUseServer() || this.config == null || this.config.getUseServer()) {
            return FSitMod.getConfig();
        }

        return this.config;
    }

    @Override
    public boolean fsit$hasConfig() {
        return this.config != null;
    }

    @Override
    public void fsit$setPose(@NotNull PlayerPose pose, @Nullable Vec3d pos) {
        super.fsit$setPose(pose, pos);

        UpdatePoseCallback.EVENT.invoker().updatePose((ServerPlayerEntity) (Object) this, pose, pos);
    }

    @Override
    public void fsit$startCrawling(@NotNull CrawlEntity crawlEntity) {
        this.crawlEntity = crawlEntity;
    }

    @Override
    public void fsit$stopCrawling() {
        if (this.crawlEntity != null) {
            this.crawlEntity.discard();
            this.crawlEntity = null;
        }
    }

    @Override
    public boolean fsit$isCrawling() {
        return this.crawlEntity != null && !this.crawlEntity.isRemoved();
    }

    @Override
    public @NotNull Vec3d fsit$getPlayerVelocity() {
        return this.playerVelocity;
    }
}

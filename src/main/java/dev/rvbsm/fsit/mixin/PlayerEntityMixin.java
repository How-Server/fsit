package dev.rvbsm.fsit.mixin;

import dev.rvbsm.fsit.api.entity.PoseableEntity;
import dev.rvbsm.fsit.entity.PlayerPose;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntityMixin implements PoseableEntity {
    @Unique
    protected @NotNull PlayerPose pose = PlayerPose.Standing;

    @Shadow
    public abstract PlayerAbilities getAbilities();

    @Shadow
    public abstract boolean isSwimming();

    @Redirect(method = "updatePose", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSwimming()Z"))
    private boolean isCrawling(PlayerEntity player) {
        return this.isSwimming() || this.fsit$isInPose(PlayerPose.Crawling);
    }

    @Override
    public void fsit$setPose(@NotNull PlayerPose pose, @Nullable Vec3d pos) {
        this.pose = pose;
    }

    @Override
    public @NotNull PlayerPose fsit$getPose() {
        return this.pose;
    }
}

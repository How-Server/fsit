package dev.rvbsm.fsit.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.rvbsm.fsit.api.event.ClientCommandCallback;
import dev.rvbsm.fsit.api.event.PassedUseBlockCallback;
import dev.rvbsm.fsit.api.event.PassedUseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onClientCommand", at = @At("TAIL"))
    public void onClientCommand(@NotNull ClientCommandC2SPacket packet, CallbackInfo ci) {
        ClientCommandCallback.EVENT.invoker().onClientMode(this.player, packet.getMode());
    }

    @ModifyVariable(method = "onPlayerInteractBlock", at = @At("STORE"))
    private ActionResult interactBlock(ActionResult interactionActionResult, @Local ServerWorld world, @Local LocalRef<Hand> handRef, @Local BlockHitResult blockHitResult) {
        if (interactionActionResult == ActionResult.PASS && handRef.get() == Hand.OFF_HAND && player.getStackInHand(handRef.get()).getUseAction().ordinal() == 0) {
            handRef.set(Hand.MAIN_HAND);

            return PassedUseBlockCallback.EVENT.invoker().interactBlock(player, world, blockHitResult);
        }

        return interactionActionResult;
    }

    @Mixin(targets = "net.minecraft.server.network.ServerPlayNetworkHandler$1")
    public abstract static class PlayerInteractEntityC2SPacketHandler {
        @Shadow
        @Final
        ServerPlayNetworkHandler field_28963;

        @Shadow
        @Final
        Entity field_28962;

        @Shadow
        @Final
        ServerWorld field_39991;

        @ModifyVariable(method = "processInteract", at = @At("STORE"))
        private ActionResult interactPlayer(ActionResult interactionActionResult, @Local(argsOnly = true) LocalRef<Hand> handRef) {
            if (interactionActionResult == ActionResult.PASS && handRef.get() == Hand.OFF_HAND && field_28963.player.getStackInHand(handRef.get()).getUseAction().ordinal() == 0) {
                handRef.set(Hand.MAIN_HAND);

                return PassedUseEntityCallback.EVENT.invoker().interactEntity(field_28963.player, field_39991, field_28962);
            }

            return interactionActionResult;
        }
    }
}

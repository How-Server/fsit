package dev.rvbsm.fsit.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.rvbsm.fsit.event.PassedUseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {

    @Inject(method = "interactBlock", at = @At(value = "RETURN"), cancellable = true)
    private void onSkipBlockInteraction(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue() == ActionResult.PASS) {
            final ActionResult result = PassedUseBlockCallback.EVENT.invoker().interactBlock(player, world, hand, hitResult);

            cir.setReturnValue(result);
        }
    }
}

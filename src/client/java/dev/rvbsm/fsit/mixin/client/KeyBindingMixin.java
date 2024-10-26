package dev.rvbsm.fsit.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.rvbsm.fsit.client.option.HybridKeyBinding;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Inject(method = "untoggleStickyKeys", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/client/option/StickyKeyBinding"))
    private static void untoggleHybridKeys(CallbackInfo ci, @Local KeyBinding keyBinding) {
        if (keyBinding instanceof HybridKeyBinding hybridKeyBinding) {
            hybridKeyBinding.untoggle$fsit_client();
        }
    }
}

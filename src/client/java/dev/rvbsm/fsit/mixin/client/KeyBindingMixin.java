package dev.rvbsm.fsit.mixin.client;

import dev.rvbsm.fsit.client.option.HybridKeyBinding;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @ModifyVariable(method = "untoggleStickyKeys", at = @At("STORE"))
    private static KeyBinding untoggleHybridKey(KeyBinding keyBinding) {
        if (keyBinding instanceof HybridKeyBinding hybridKeyBinding) {
            hybridKeyBinding.untoggle();
        }

        return keyBinding;
    }
}

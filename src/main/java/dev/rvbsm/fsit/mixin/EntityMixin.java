package dev.rvbsm.fsit.mixin;

import dev.rvbsm.fsit.FSitMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Entity.class)
public abstract class EntityMixin {

	private final FSitMod FSit = FSitMod.getInstance();
	public @Shadow World world;

	@Shadow
	public abstract boolean isOnGround();

	@Shadow
	public abstract double getX();

	@Shadow
	public abstract double getY();

	@Shadow
	public abstract double getZ();

	@Inject(at = @At(value = "TAIL"), method = "setSneaking", locals = LocalCapture.CAPTURE_FAILHARD)
	public void setSneaking(boolean sneaking, CallbackInfo ci) {
		if (this.world.isClient) return;
		if (!this.isOnGround()) return;

		if ((Entity) (Object) this instanceof PlayerEntity player) if (FSit.isNeedSeat(player) && !sneaking)
			FSit.spawnSeat(player, this.world, this.getX(), this.getY(), this.getZ());
		else FSit.addSneaked(player);
	}
}

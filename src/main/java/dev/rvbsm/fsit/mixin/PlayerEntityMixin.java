package dev.rvbsm.fsit.mixin;

import dev.rvbsm.fsit.FSitMod;
import dev.rvbsm.fsit.entity.CrawlEntity;
import dev.rvbsm.fsit.entity.PlayerPose;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final BlockState BARRIER = Blocks.BARRIER.getDefaultState();
	private BlockPos blockAbove = null;
	private CrawlEntity entityAbove = null;

	@Inject(method = "updatePose", at = @At("HEAD"))
	public void updatePose(CallbackInfo ci) {
		if ((PlayerEntity) (Object) this instanceof ServerPlayerEntity player) {
			final World world = player.getWorld();
			final BlockPos blockPos = player.getBlockPos().up();
//?			if (player.getMovementSpeed() > something) blockPos = blockPos.offset(player.getMovementDirection());
			final Vec3d entityPos = blockPos.toCenterPos();

			final boolean placeBarrier = world.getBlockState(blockPos).isAir();
			final boolean placeShulker = !placeBarrier && !world.getBlockState(blockPos).isSideSolidFullSquare(world, blockPos, Direction.DOWN);

			if (this.blockAbove != null) {
				player.networkHandler.sendPacket(new BlockUpdateS2CPacket(this.blockAbove, AIR));
				this.blockAbove = null;
			}
			if (this.entityAbove != null && !placeShulker) {
				player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(this.entityAbove.getId()));
				this.entityAbove = null;
			}

			if (FSitMod.isInPose(player.getUuid(), PlayerPose.CRAWL)) {
				player.setSwimming(true);
				if (FSitMod.isModded(player.getUuid())) return;

				if (placeShulker) {
					if (this.entityAbove == null) {
						this.entityAbove = new CrawlEntity(world, entityPos);
						player.networkHandler.sendPacket(this.entityAbove.createSpawnPacket());
						player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(this.entityAbove.getId(), this.entityAbove.getDataTracker().getChangedEntries()));
					} else if (!this.entityAbove.getPos().equals(entityPos)) {
						this.entityAbove.setPosition(entityPos);
						player.networkHandler.sendPacket(new EntityPositionS2CPacket(this.entityAbove));
					}
				} else if (placeBarrier) {
					this.blockAbove = blockPos;
					player.networkHandler.sendPacket(new BlockUpdateS2CPacket(blockPos, BARRIER));
				}
			}
		}
	}
}

package dev.rvbsm.fsit.event.client;

import dev.rvbsm.fsit.packet.SpawnSeatC2SPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FluidModificationItem;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public abstract class InteractBlockCallback {

	public static ActionResult interactBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		final Item handItem = player.getStackInHand(hand).getItem();
		if (handItem instanceof BlockItem) return ActionResult.PASS;
		else if (handItem instanceof FluidModificationItem) return ActionResult.PASS;
		else if (!player.isOnGround() && player.shouldCancelInteraction()) return ActionResult.PASS;

		if (dev.rvbsm.fsit.event.InteractBlockCallback.isSittable(world, hitResult)) {
			SpawnSeatC2SPacket.send(player.getPos(), hitResult);

			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}
}
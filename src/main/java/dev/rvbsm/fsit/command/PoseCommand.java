package dev.rvbsm.fsit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import dev.rvbsm.fsit.FSitMod;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Consumer;

public record PoseCommand(String name, Consumer<ServerPlayerEntity> poseConsumer)
				implements Commandish<ServerCommandSource> {

	@Override
	public boolean requires(ServerCommandSource src) {
		return src.isExecutedByPlayer();
	}

	@Override
	public int command(CommandContext<ServerCommandSource> ctx) {
		final ServerCommandSource src = ctx.getSource();
		final ServerPlayerEntity player = src.getPlayer();
		if (player == null) return -1;

		if (FSitMod.isPosing(player.getUuid())) FSitMod.resetPose(player);
		else poseConsumer.accept(player);

		return Command.SINGLE_SUCCESS;
	}
}
package dev.rvbsm.fsit.api.event;

import dev.rvbsm.fsit.entity.PlayerPose;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface UpdatePoseCallback {
    Event<UpdatePoseCallback> EVENT = EventFactory.createArrayBacked(UpdatePoseCallback.class, (listeners) -> (player, pose, pos) -> {
        for (UpdatePoseCallback listener : listeners) {
            listener.updatePose(player, pose, pos);
        }
    });

    void updatePose(@NotNull ServerPlayerEntity player, @NotNull PlayerPose pose, @Nullable Vec3d pos);
}
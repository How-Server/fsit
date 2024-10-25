package dev.rvbsm.fsit.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PassedUseEntityCallback {
    Event<PassedUseEntityCallback> EVENT = EventFactory.createArrayBacked(PassedUseEntityCallback.class, (listeners) -> (player, world, entity) -> {
        for (PassedUseEntityCallback listener : listeners) {
            var result = listener.interactEntity(player, world, entity);

            if (result != ActionResult.PASS) {
                return result;
            }
        }

        return ActionResult.PASS;
    });

    ActionResult interactEntity(@NotNull ServerPlayerEntity player, @NotNull ServerWorld world, @NotNull Entity entity);
}

package dev.rvbsm.fsit.api.player;

import dev.rvbsm.fsit.config.ModConfig;
import org.jetbrains.annotations.NotNull;

public interface PlayerConfig {

    void fsit$setConfig(@NotNull ModConfig config);

    @NotNull ModConfig fsit$getConfig();

    boolean fsit$hasConfig();
}

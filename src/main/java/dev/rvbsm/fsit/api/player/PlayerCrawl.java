package dev.rvbsm.fsit.api.player;

import dev.rvbsm.fsit.entity.CrawlEntity;
import org.jetbrains.annotations.NotNull;

public interface PlayerCrawl {

    void fsit$startCrawling(@NotNull CrawlEntity crawlEntity);

    void fsit$stopCrawling();

    boolean fsit$isCrawling();
}

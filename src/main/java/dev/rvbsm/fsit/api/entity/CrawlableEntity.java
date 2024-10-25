package dev.rvbsm.fsit.api.entity;

import dev.rvbsm.fsit.entity.CrawlEntity;
import org.jetbrains.annotations.NotNull;

public interface CrawlableEntity {
    void fsit$startCrawling(@NotNull CrawlEntity crawlEntity);

    void fsit$stopCrawling();

    boolean fsit$isCrawling();
}

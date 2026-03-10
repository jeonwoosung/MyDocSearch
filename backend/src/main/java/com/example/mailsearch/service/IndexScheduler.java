package com.example.mailsearch.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IndexScheduler {
    private final IndexService indexService;

    public IndexScheduler(IndexService indexService) {
        this.indexService = indexService;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void runIncrementalIndexAtNight() {
        try {
            indexService.update(null);
        } catch (RuntimeException ignored) {
            // Scheduler should not crash the app; errors remain in application logs.
        }
    }
}

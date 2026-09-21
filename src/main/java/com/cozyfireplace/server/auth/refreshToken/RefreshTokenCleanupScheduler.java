package com.cozyfireplace.server.auth.refreshToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Планировщик для очистки истёкших refresh-токенов.
 * Запускается каждые 24 часа в 03:00 ночи (по умолчанию).
 * Можно настроить через свойство refresh.token.cleanup.cron
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;


    @Scheduled(cron = "${refresh.token.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Запуск очистки истёкших refresh-токенов");
        
        int deletedCount = refreshTokenRepository.deleteExpiredAndInactiveTokens();
        
        log.info("Удалено {} истёкших refresh-токенов", deletedCount);
    }
}

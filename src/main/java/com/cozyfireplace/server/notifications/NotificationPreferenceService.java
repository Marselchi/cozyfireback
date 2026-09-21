package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.notifications.dto.NotificationMapper;
import com.cozyfireplace.server.notifications.dto.NotificationPreferenceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    private final NotificationMapper notificationMapper;

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<NotificationPreferenceRequest> getAll(Account account) {
        return repository.findAllByAccountId(account.getId()).stream().map(notificationMapper::toRequestDto).toList();
    }
    @Transactional
    public void replaceAll(Account account, List<NotificationPreferenceRequest> request) {
        //optimization sheneniganz
        String sql = """
            INSERT INTO notification_preferences (account_id, event_code, channel, enabled)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (account_id, event_code, channel) DO UPDATE SET enabled = EXCLUDED.enabled
            """;

        jdbcTemplate.batchUpdate(sql, request.stream()
                .map(r -> new Object[]{account.getId(), r.eventCode().name(), r.channel().name(), r.enabled()})
                .toList());
    }
}
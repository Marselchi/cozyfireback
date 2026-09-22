package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.notifications.dto.NotificationChannel;
import com.cozyfireplace.server.notifications.dto.NotificationEventCode;
import com.cozyfireplace.server.notifications.dto.NotificationMapper;
import com.cozyfireplace.server.notifications.dto.NotificationPreferenceRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationPreferenceService}.
 * <p>
 * The repository, {@link NotificationMapper} and {@link JdbcTemplate} are mocked. Covers the
 * preference listing projection and the upsert-batch replacement, asserting the flattened
 * {@code [accountId, eventCode, channel, enabled]} argument rows handed to the JDBC batch.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPreferenceService")
class NotificationPreferenceServiceTest {

    private static final Long ACCOUNT_ID = 7L;

    @Mock
    private NotificationPreferenceRepository repository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private NotificationPreferenceService service;

    @Test
    @DisplayName("getAll projects the stored preferences")
    void getAll() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(ACCOUNT_ID);
        NotificationPreference preference = mock(NotificationPreference.class);
        when(repository.findAllByAccountId(ACCOUNT_ID)).thenReturn(List.of(preference));
        NotificationPreferenceRequest projected = mock(NotificationPreferenceRequest.class);
        when(notificationMapper.toRequestDto(preference)).thenReturn(projected);

        assertThat(service.getAll(account)).containsExactly(projected);
    }

    @Test
    @DisplayName("replaceAll flattens each request into a JDBC batch row")
    @SuppressWarnings("unchecked")
    void replaceAll() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(ACCOUNT_ID);
        List<NotificationPreferenceRequest> requests = List.of(
                new NotificationPreferenceRequest(NotificationEventCode.QA_ANSWERED, NotificationChannel.PUSH, true),
                new NotificationPreferenceRequest(NotificationEventCode.SESSION_CREATED, NotificationChannel.POLLING, false));

        service.replaceAll(account, requests);

        ArgumentCaptor<List<Object[]>> captor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture());
        List<Object[]> rows = captor.getValue();
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsExactly(ACCOUNT_ID, "QA_ANSWERED", "PUSH", true);
        assertThat(rows.get(1)).containsExactly(ACCOUNT_ID, "SESSION_CREATED", "POLLING", false);
    }
}

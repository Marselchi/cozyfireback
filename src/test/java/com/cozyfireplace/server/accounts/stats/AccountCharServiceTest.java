package com.cozyfireplace.server.accounts.stats;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountChar;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.util.exception.NotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AccountCharService}.
 * <p>
 * All repositories and the {@link AccountCharMapper} are mocked; the character entity is real so
 * the D&amp;D 5e stat-modifier math and the create/update wiring are genuinely exercised. Covers
 * the read/self-read lookups, character creation (level default and stat modifier) and the update
 * flow including its optimistic-locking guard.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountCharService")
class AccountCharServiceTest {

    private static final Long CHAR_ID = 5L;

    @Mock
    private AccountCharRepository accountCharRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private StatRepository statRepository;
    @Mock
    private AccountCharMapper accountCharMapper;

    @InjectMocks
    private AccountCharService service;

    @Nested
    @DisplayName("getCharacter")
    class Get {

        @Test
        @DisplayName("returns the mapper projection")
        void projects() {
            AccountChar character = AccountChar.builder().id(CHAR_ID).build();
            when(accountCharRepository.findByIdWithStatsAndSkills(CHAR_ID)).thenReturn(Optional.of(character));
            CharacterResponse response = mock(CharacterResponse.class);
            when(accountCharMapper.toResponse(character)).thenReturn(response);

            assertThat(service.getCharacter(CHAR_ID)).isSameAs(response);
        }

        @Test
        @DisplayName("throws when the character is unknown")
        void missing() {
            when(accountCharRepository.findByIdWithStatsAndSkills(CHAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCharacter(CHAR_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getCharacterSelf")
    class GetSelf {

        @Test
        @DisplayName("throws when the account has no character")
        void noCharacter() {
            Account account = mock(Account.class);
            when(account.getAccountChar()).thenReturn(null);

            assertThatThrownBy(() -> service.getCharacterSelf(account))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws when the stored character disappeared")
        void characterMissing() {
            Account account = mock(Account.class);
            when(account.getAccountChar()).thenReturn(AccountChar.builder().id(CHAR_ID).build());
            when(accountCharRepository.findByIdWithStatsAndSkills(CHAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCharacterSelf(account))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("projects the account's own character")
        void projects() {
            Account account = mock(Account.class);
            when(account.getAccountChar()).thenReturn(AccountChar.builder().id(CHAR_ID).build());
            AccountChar character = AccountChar.builder().id(CHAR_ID).build();
            when(accountCharRepository.findByIdWithStatsAndSkills(CHAR_ID)).thenReturn(Optional.of(character));
            CharacterResponse response = mock(CharacterResponse.class);
            when(accountCharMapper.toResponse(character)).thenReturn(response);

            assertThat(service.getCharacterSelf(account)).isSameAs(response);
        }
    }

    @Nested
    @DisplayName("createCharacter")
    class Create {

        @Test
        @DisplayName("defaults level to 1 and attaches the character to the account")
        void defaultsAndWires() {
            CharacterCreateRequest request = CharacterCreateRequest.builder().name("Nyra").build();
            Account account = mock(Account.class);
            AccountChar saved = AccountChar.builder().id(CHAR_ID).build();
            when(accountCharRepository.save(any(AccountChar.class))).thenReturn(saved);
            CharacterResponse response = mock(CharacterResponse.class);
            when(accountCharMapper.toResponse(saved)).thenReturn(response);

            assertThat(service.createCharacter(request, account)).isSameAs(response);

            ArgumentCaptor<AccountChar> captor = ArgumentCaptor.forClass(AccountChar.class);
            verify(accountCharRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Nyra");
            assertThat(captor.getValue().getLevel()).isEqualTo(1);
            verify(account).setAccountChar(saved);
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("computes the D&D 5e modifier for a provided stat block")
        void computesStatModifier() {
            CharacterCreateRequest request = CharacterCreateRequest.builder()
                    .name("Nyra").level(1)
                    .stats(List.of(StatBlockResponse.builder().key("str").score(14).build()))
                    .build();
            when(statRepository.findById("str"))
                    .thenReturn(Optional.of(Stat.builder().key("str").system(GameSystem.DND_5E).build()));
            AccountChar saved = AccountChar.builder().id(CHAR_ID).build();
            when(accountCharRepository.save(any(AccountChar.class))).thenReturn(saved);
            when(accountCharMapper.toResponse(any(AccountChar.class))).thenReturn(mock(CharacterResponse.class));

            service.createCharacter(request, mock(Account.class));

            ArgumentCaptor<AccountChar> captor = ArgumentCaptor.forClass(AccountChar.class);
            verify(accountCharRepository).save(captor.capture());
            AccountCharStat stat = captor.getValue().getStats().iterator().next();
            assertThat(stat.getScore()).isEqualTo(14);
            assertThat(stat.getModifier()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("updateCharacter")
    class Update {

        @Test
        @DisplayName("throws when the character is unknown")
        void missing() {
            when(accountCharRepository.findByIdWithStatsAndSkills(CHAR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCharacter(CHAR_ID, CharacterUpdateRequest.builder().build(), mock(Account.class)))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("throws when the supplied version is stale")
        void versionMismatch() {
            AccountChar character = AccountChar.builder().id(CHAR_ID).version(1).build();
            when(accountCharRepository.findByIdWithStatsAndSkills(CHAR_ID)).thenReturn(Optional.of(character));
            CharacterUpdateRequest request = CharacterUpdateRequest.builder().version(2).name("Nyra").build();

            assertThatThrownBy(() -> service.updateCharacter(CHAR_ID, request, mock(Account.class)))
                    .isInstanceOf(OptimisticLockException.class);
            verify(accountCharRepository, never()).save(any());
        }

        @Test
        @DisplayName("applies non-null fields and saves")
        void appliesAndSaves() {
            AccountChar character = AccountChar.builder().id(CHAR_ID).name("old").build();
            when(accountCharRepository.findByIdWithStatsAndSkills(CHAR_ID)).thenReturn(Optional.of(character));
            when(accountCharRepository.save(character)).thenReturn(character);
            CharacterResponse response = mock(CharacterResponse.class);
            when(accountCharMapper.toResponse(character)).thenReturn(response);
            CharacterUpdateRequest request = CharacterUpdateRequest.builder().name("Renamed").build();

            assertThat(service.updateCharacter(CHAR_ID, request, mock(Account.class))).isSameAs(response);
            assertThat(character.getName()).isEqualTo("Renamed");
        }
    }
}

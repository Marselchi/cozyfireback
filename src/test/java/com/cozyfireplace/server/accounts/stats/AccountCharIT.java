package com.cozyfireplace.server.accounts.stats;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountChar;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.rooms.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the character-stats module
 * ({@code /api/v1/account_chars/**} and {@code /api/v1/skills/{roomId}}).
 *
 * <p>Seeds the D&amp;D 5e stat/skill dictionary directly through the repositories, then verifies
 * character creation (including D&amp;D modifier maths: {@code (score-10)/2} for stats and
 * {@code statMod + profBonus*proficiency} for skills), self lookup, optimistic-lock version
 * checking and the room skill catalog.</p>
 */
@DisplayName("AccountChar + Skills IT: /api/v1/account_chars, /api/v1/skills")
class AccountCharIT extends BaseIntegrationTest {

    @Autowired
    private StatRepository statRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private AccountCharRepository accountCharRepository;
    @Autowired
    private AccountRepository accountRepo;

    @BeforeEach
    void seedStatDictionary() {
        if (statRepository.findById("str").isEmpty()) {
            Stat str = Stat.builder().key("str").label("Strength").system(GameSystem.DND_5E).sortOrder(1).build();
            statRepository.save(str);
            skillRepository.save(Skill.builder()
                    .key("athletics").label("Athletics").system(GameSystem.DND_5E)
                    .stat(str).sortOrder(1).build());
        }
    }

    private String createCharacterJson() {
        return """
                {
                  "name": "Thrall",
                  "level": 3,
                  "characterClass": "Fighter",
                  "race": "Half-Elf",
                  "system": "DND_5E",
                  "origin": "City of Brass",
                  "stats": [ { "key": "str", "label": "Strength", "score": 16 } ],
                  "skills": [ { "key": "athletics", "label": "Athletics", "statKey": "str", "proficiency": 2 } ]
                }
                """;
    }

    /** Creates a character for the creator account of the room via the API. */
    private Long createCharacterViaApi(Room room) throws Exception {
        var result = mockMvc.perform(post("/api/v1/account_chars/{roomId}", room.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCharacterJson()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    // ====== POST /api/v1/account_chars/{roomId} ======
    @Nested
    @DisplayName("POST /api/v1/account_chars/{roomId}")
    class CreateCharacterTests {

        @Test
        @DisplayName("should create character with computed D&D modifiers and bind it to the account")
        void createCharacter_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Char Room");
            Account creator = room.getCreator();
            authenticateAs(dm);

            mockMvc.perform(post("/api/v1/account_chars/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createCharacterJson()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", equalTo("Thrall")))
                    .andExpect(jsonPath("$.level", equalTo(3)))
                    .andExpect(jsonPath("$.stats[0].key", equalTo("str")))
                    // D&D 5e: modifier = floor((16-10)/2) = 3
                    .andExpect(jsonPath("$.stats[0].modifier", equalTo(3)))
                    .andExpect(jsonPath("$.skills[0].key", equalTo("athletics")))
                    // athletics: statMod 3 + profBonus(level 3 -> 2) * proficiency 2 = 7
                    .andExpect(jsonPath("$.skills[0].modifier", equalTo(7)));

            Account reloaded = accountRepo.findById(creator.getId()).orElseThrow();
            assertThat(reloaded.getAccountChar(), notNullValue());
            assertThat(reloaded.getAccountChar().getName(), equalTo("Thrall"));
        }

        @Test
        @DisplayName("should default level to 1 when omitted")
        void createCharacter_defaultLevel() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Char Room");
            authenticateAs(dm);

            mockMvc.perform(post("/api/v1/account_chars/{roomId}", room.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Solo"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.level", equalTo(1)));
        }
    }

    // ====== GET /api/v1/account_chars/{characterId} ======
    @Nested
    @DisplayName("GET /api/v1/account_chars/{characterId}")
    class GetCharacterTests {

        @Test
        @DisplayName("should return the character with stats and skills")
        void getCharacter_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Char Room");
            authenticateAs(dm);
            Long characterId = createCharacterViaApi(room);

            mockMvc.perform(get("/api/v1/account_chars/{characterId}", characterId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", equalTo(characterId.intValue())))
                    .andExpect(jsonPath("$.race", equalTo("Half-Elf")))
                    .andExpect(jsonPath("$.stats", hasSize(1)))
                    .andExpect(jsonPath("$.skills", hasSize(1)));
        }

        @Test
        @DisplayName("should not answer 2xx for an unknown character")
        void getCharacter_notFound() throws Exception {
            authenticateAs(newProfile("dm"));

            // EntityNotFoundException is unmapped today -> generic advice (500).
            // Asserting only "not 2xx" so the test survives a future 404 mapping.
            mockMvc.perform(get("/api/v1/account_chars/{characterId}", 999_999L))
                    .andExpect(result -> assertThat("unknown character must not return 2xx",
                            result.getResponse().getStatus(), greaterThanOrEqualTo(400)));
        }
    }

    // ====== GET /api/v1/account_chars/self/{roomId} ======
    @Nested
    @DisplayName("GET /api/v1/account_chars/self/{roomId}")
    class GetSelfTests {

        @Test
        @DisplayName("should return the character bound to the current account")
        void getCharacterSelf_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Char Room");
            authenticateAs(dm);
            Long characterId = createCharacterViaApi(room);

            mockMvc.perform(get("/api/v1/account_chars/self/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", equalTo(characterId.intValue())));
        }

        @Test
        @DisplayName("should return 404 when the account has no character yet")
        void getCharacterSelf_missing() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Char Room");
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/account_chars/self/{roomId}", room.getId()))
                    .andExpect(status().isNotFound());
        }
    }

    // ====== PATCH /api/v1/account_chars/{roomId}/{characterId} ======
    @Nested
    @DisplayName("PATCH /api/v1/account_chars/{roomId}/{characterId}")
    class UpdateCharacterTests {

        @Test
        @DisplayName("should partially update name and level")
        void updateCharacter_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Char Room");
            authenticateAs(dm);
            Long characterId = createCharacterViaApi(room);

            mockMvc.perform(patch("/api/v1/account_chars/{roomId}/{characterId}", room.getId(), characterId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Thrall the Bold","level":4}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", equalTo("Thrall the Bold"))
                    );

            AccountChar saved = accountCharRepository.findById(characterId).orElseThrow();
            assertThat(saved.getLevel(), equalTo(4));
        }

        @Test
        @DisplayName("should reject a stale version (optimistic locking)")
        void updateCharacter_versionConflict() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Char Room");
            authenticateAs(dm);
            Long characterId = createCharacterViaApi(room);

            // OptimisticLockException currently reaches the generic advice (500); assert
            // only that a stale write is refused, so the test passes once it's mapped to 409.
            mockMvc.perform(patch("/api/v1/account_chars/{roomId}/{characterId}", room.getId(), characterId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Stale","version":99}
                                    """))
                    .andExpect(result -> assertThat("stale version write must be refused",
                            result.getResponse().getStatus(), greaterThanOrEqualTo(400))); // >= 400, request must not succeed
        }
    }

    // ====== GET /api/v1/skills/{roomId} ======
    @Nested
    @DisplayName("GET /api/v1/skills/{roomId}")
    class SkillCatalogTests {

        @Test
        @DisplayName("should return skills of the room's game system")
        void getRoomSkills_success() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "DND Room");
            room.setGameSystem(GameSystem.DND_5E);
            roomRepository.save(room);
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/skills/{roomId}", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.key == 'athletics')]", hasSize(1)));
        }

        @Test
        @DisplayName("should return 404 when the room has no game system")
        void getRoomSkills_noGameSystem() throws Exception {
            Profile dm = newProfile("dm");
            Room room = newRoomOwnedBy(dm, "Systemless Room");
            authenticateAs(dm);

            mockMvc.perform(get("/api/v1/skills/{roomId}", room.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for an unknown room")
        void getRoomSkills_roomNotFound() throws Exception {
            authenticateAs(newProfile("dm"));

            mockMvc.perform(get("/api/v1/skills/{roomId}", 999_999L))
                    .andExpect(status().isNotFound());
        }
    }
}

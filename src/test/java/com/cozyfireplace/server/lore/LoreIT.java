package com.cozyfireplace.server.lore;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileRepository;
import com.cozyfireplace.server.lore.dto.*;
import com.cozyfireplace.server.loreViews.LoreViewService;
import com.cozyfireplace.server.notifications.firebase.FirebaseConfig;
import com.cozyfireplace.server.rooms.Room;
import com.cozyfireplace.server.rooms.RoomRepository;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleRepository;
import com.cozyfireplace.server.tags.Tag;
import com.cozyfireplace.server.tags.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("LoreController integration tests (Profile-based auth)")
class LoreIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LoreRepository loreRepository;
    @Autowired
    private RoomRepository roomRepository;
    @MockitoBean
    private FirebaseConfig firebaseConfig; // или какой там тип у твоего конфига
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TagRepository tagRepository;

    @MockitoBean
    private LoreViewService loreViewService;

    private Profile testProfile;
    private Profile creatorProfile;
    private Room testRoom;
    private Account testAccount;      // Profile → Account в комнате (обычный участник)
    private Account creatorAccount;   // Profile → Account в комнате (создатель)
    private Role testRole;
    private Tag testTag;

    @BeforeEach
    void setUp() {
        // 1. Создаём профили
        testProfile = new Profile();
        testProfile.setEmail("player@test.com");
        testProfile.setUsername("player");
        testProfile.setPassword("password");
        testProfile = profileRepository.save(testProfile);

        creatorProfile = new Profile();
        creatorProfile.setEmail("dm@test.com");
        creatorProfile.setUsername("dmmmm");
        creatorProfile.setPassword("password");
        creatorProfile = profileRepository.save(creatorProfile);

        // 2. Создаём комнату (пока без создателя)
        testRoom = Room.builder()
                .name("Test Room")
                .url("/test-room")
                .description(null)
                .build();
        // Не сохраняем комнату сразу!

        // 3. Создаём аккаунт создателя, привязанный к комнате
        creatorAccount = Account.builder()
                .name("DM")
                .profile(creatorProfile)
                .room(testRoom)
                .build();
        creatorAccount = accountRepository.save(creatorAccount); // сначала сохраняем аккаунт

        // 4. Теперь устанавливаем создателя комнаты и сохраняем комнату
        testRoom.setCreator(creatorAccount);
        testRoom = roomRepository.save(testRoom); // теперь комната сохранена с корректным creator

        // 5. Создаём обычного участника (привязан к уже сохранённой комнате)
        testAccount = new Account();
        testAccount.setName("Player");
        testAccount.setProfile(testProfile);
        testAccount.setRoom(testRoom);
        testAccount = accountRepository.save(testAccount);

        // 6. Создаём роль и тег в комнате
        testRole = new Role();
        testRole.setName("Test Role");
        testRole.setRoom(testRoom);
        testRole = roleRepository.save(testRole);

        testTag = new Tag();
        testTag.setName("test-tag");
        testTag.setRoom(testRoom);
        testTag = tagRepository.save(testTag);
    }

    // ====== HELPERS: настройка аутентификации ======

    /**
     * Устанавливает в SecurityContext аутентификацию для заданного профиля.
     * Резолвер @CurrentAccount найдёт аккаунт по (profileId, roomId) из запроса.
     */
    private void authenticateAs(Profile profile) {
        var auth = new UsernamePasswordAuthenticationToken(
                profile.getId(),          // principal = profileId
                null,                     // credentials
                List.of()                 // authorities (если нужны)
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // ====== CREATE LORE ======
    @Nested
    @DisplayName("POST /api/v1/lore/{roomId}")
    class CreateLoreTests {

//        @Test
//        @DisplayName("should create lore and return 201 with ID")
//        void createLore_success() throws Exception {
//            authenticateAs(creatorProfile); // Авторизуемся как создатель
//
//            LoreRequest request = new LoreRequest("New Lore", "Content", null, null, null, null);
//
//            try {
//                mockMvc.perform(post("/api/v1/lore/{roomId}", testRoom.getId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(request)))
//                        .andExpect(status().isCreated())
//                        .andExpect(content().string(not(emptyString())));
//
//                var lores = loreRepository.findAllByRoomId(testRoom.getId());
//                assertThat(lores, hasSize(1));
//                assertThat(lores.getFirst().getTitle(), equalTo("New Lore"));
//            } finally {
//                clearAuthentication();
//            }
//        }
//
//        @Test
//        @DisplayName("should return 404 when room not found")
//        void createLore_roomNotFound() throws Exception {
//            authenticateAs(creatorProfile);
//
//            LoreRequest request = new LoreRequest("Lore", "Content", null, null, null, null);
//
//            try {
//                mockMvc.perform(post("/api/v1/lore/{roomId}", 99999L)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(request)))
//                        .andExpect(status().isNotFound());
//            } finally {
//                clearAuthentication();
//            }
//        }
//
//        @Test
//        @DisplayName("should create lore with roles and tags")
//        void createLore_withRolesAndTags() throws Exception {
//            authenticateAs(creatorProfile);
//
//            LoreRequest request = new LoreRequest(
//                    "Lore with meta",
//                    "Description",
//                    "date",
//                    "Content",
//                    Set.of(testRole.getId()),
//                    Set.of(testTag.getId())
//            );
//
//            try {
//                mockMvc.perform(post("/api/v1/lore/{roomId}", testRoom.getId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(request)))
//                        .andExpect(status().isCreated());
//
//                var savedLore = loreRepository.findAllByRoomId(testRoom.getId()).get(0);
//                assertThat(savedLore.getRoles(), hasItem(testRole));
//                assertThat(savedLore.getTags(), hasItem(testTag));
//            } finally {
//                clearAuthentication();
//            }
//        }
//
//        @Test
//        @DisplayName("should return 403 when non-creator tries to create")
//        void createLore_forbidden() throws Exception {
//            authenticateAs(testProfile); // Обычный пользователь, не создатель
//
//            LoreRequest request = new LoreRequest("Lore", "Content", null, null, null, null);
//
//            try {
//                mockMvc.perform(post("/api/v1/lore/{roomId}", testRoom.getId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(request)))
//                        .andExpect(status().isForbidden()); // или 403, зависит от реализации
//            } finally {
//                clearAuthentication();
//            }
//        }
//    }

        // ====== UPDATE LORE ======
//    @Nested
//    @DisplayName("PUT /api/v1/lore/{roomId}/{loreId}")
//    class UpdateLoreTests {
//
//        @Test
//        @DisplayName("should update lore and return 204")
//        void updateLore_success() throws Exception {
//            authenticateAs(creatorProfile);
//
//            // Создаём лор заранее
//            Lore initialLore = new Lore();
//            initialLore.setTitle("Old Title");
//            initialLore.setContent("Old Content");
//            initialLore.setAccount(creatorAccount);
//            Long loreId = loreRepository.save(initialLore).getId();
//
//            LoreRequest updateRequest = new LoreRequest("New Title", "New Content", null, "content", null, null);
//
//            try {
//                mockMvc.perform(put("/api/v1/lore/{roomId}/{loreId}", testRoom.getId(), loreId)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(updateRequest)))
//                        .andExpect(status().isNoContent());
//
//                var updated = loreRepository.findById(loreId).orElseThrow();
//                assertThat(updated.getTitle(), equalTo("New Title"));
//                assertThat(updated.getContent(), equalTo("New Content"));
//            } finally {
//                clearAuthentication();
//            }
//        }
//
//        @Test
//        @DisplayName("should return 404 when lore not found")
//        void updateLore_notFound() throws Exception {
//            authenticateAs(creatorProfile);
//
//            LoreRequest request = new LoreRequest("Title", "Content", null, null, null, null);
//
//            try {
//                mockMvc.perform(put("/api/v1/lore/{roomId}/{loreId}", testRoom.getId(), 99999L)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(request)))
//                        .andExpect(status().isNotFound());
//            } finally {
//                clearAuthentication();
//            }
//        }
//    }

        // ====== DELETE LORE ======
        @Nested
        @DisplayName("DELETE /api/v1/lore/{loreId}")
        class DeleteLoreTests {

            @Test
            @DisplayName("should delete lore and return 204")
            void deleteLore_success() throws Exception {
                authenticateAs(creatorProfile);

                Lore lore = new Lore();
                lore.setTitle("To Delete");
                lore.setAccount(creatorAccount);
                Long loreId = loreRepository.save(lore).getId();

                try {
                    mockMvc.perform(delete("/api/v1/lore/{loreId}", loreId))
                            .andExpect(status().isNoContent());

                    assertThat(loreRepository.findById(loreId).isPresent(), equalTo(false));
                } finally {
                    clearAuthentication();
                }
            }
        }

        // ====== GET LORE LIST ======
        @Nested
        @DisplayName("GET /api/v1/lore/{roomId}/all")
        class GetLoreListTests {

            @Test
            @DisplayName("should return page of lores for authenticated user")
            void getLoreList_success() throws Exception {
                authenticateAs(testProfile); // Обычный пользователь

                // Создаём лоры от имени создателя
                for (int i = 0; i < 3; i++) {
                    Lore lore = new Lore();
                    lore.setTitle("Lore " + i);
                    lore.setContent("Content " + i);
                    lore.setAccount(creatorAccount);
                    loreRepository.save(lore);
                }

                try {
                    mockMvc.perform(get("/api/v1/lore/{roomId}/all", testRoom.getId())
                                    .param("page", "0")
                                    .param("size", "10"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.content", hasSize(3)))
                            .andExpect(jsonPath("$.totalElements", equalTo(3)));
                } finally {
                    clearAuthentication();
                }
            }

            @Test
            @DisplayName("should filter by title")
            void getLoreList_filterByTitle() throws Exception {
                authenticateAs(testProfile);

                Lore lore1 = new Lore();
                lore1.setTitle("Important Lore");
                lore1.setAccount(creatorAccount);
                Lore lore2 = new Lore();
                lore2.setTitle("Other Lore");
                lore2.setAccount(creatorAccount);
                loreRepository.saveAll(List.of(lore1, lore2));

                try {
                    mockMvc.perform(get("/api/v1/lore/{roomId}/all", testRoom.getId())
                                    .param("title", "Important"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.content", hasSize(1)))
                            .andExpect(jsonPath("$.content[0].title", equalTo("Important Lore")));
                } finally {
                    clearAuthentication();
                }
            }


            // ====== GET LORE FOR EDIT ======
            @Nested
            @DisplayName("GET /api/v1/lore/{roomId}/{loreId}/edit")
            class GetLoreForEditTests {

                @Test
                @DisplayName("should return lore for edit with roles and tags")
                void getLoreForEdit_success() throws Exception {
                    authenticateAs(creatorProfile);

                    Lore lore = new Lore();
                    lore.setTitle("Edit Me");
                    lore.setContent("Content");
                    lore.setAccount(creatorAccount);
                    lore.setRoles(Set.of(testRole));
                    lore.setTags(Set.of(testTag));
                    Long loreId = loreRepository.save(lore).getId();

                    try {
                        mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/edit", testRoom.getId(), loreId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title", equalTo("Edit Me")))
                                .andExpect(jsonPath("$.roles", hasSize(1)))
                                .andExpect(jsonPath("$.roles[0].id", equalTo(testRole.getId().intValue())))
                                .andExpect(jsonPath("$.tags", hasSize(1)));
                    } finally {
                        clearAuthentication();
                    }
                }

                @Test
                @DisplayName("should return 404 when non-creator accesses non-public lore")
                void getLoreForEdit_forbidden() throws Exception {
                    authenticateAs(testProfile);

                    Lore lore = new Lore();
                    lore.setTitle("Hidden");
                    lore.setRoles(Set.of(testRole));
                    lore.setAccount(creatorAccount);
                    Long loreId = loreRepository.save(lore).getId();

                    try {
                        mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/edit", testRoom.getId(), loreId))
                                .andExpect(status().isNotFound());
                    } finally {
                        clearAuthentication();
                    }
                }
            }

            // ====== GET LORE FOR USER VIEW ======
            @Nested
            @DisplayName("GET /api/v1/lore/{roomId}/{loreId}/view")
            class GetLoreForUserTests {

                @Test
                @DisplayName("should return lore for user view")
                void getLoreForUser_success() throws Exception {
                    authenticateAs(testProfile);

                    Lore lore = new Lore();
                    lore.setTitle("View Me");
                    lore.setContent("Public content");
                    lore.setAccount(creatorAccount);
                    Long loreId = loreRepository.save(lore).getId();

                    try {
                        mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/view", testRoom.getId(), loreId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title", equalTo("View Me")))
                                .andExpect(jsonPath("$.content", equalTo("Public content")));

                        verify(loreViewService).setViewed(eq(testAccount.getId()), eq(loreId));
                    } finally {
                        clearAuthentication();
                    }
                }


            }

            // ====== GET LORE INLINE ======
            @Nested
            @DisplayName("GET /api/v1/lore/{roomId}/{loreId}/viewInline")
            class GetLoreInlineTests {

                @Test
                @DisplayName("should return inline response with excerpts")
                void getLoreForUserInline_success() throws Exception {
                    authenticateAs(testProfile);

                    Lore lore = new Lore();
                    lore.setTitle("Inline Lore");
                    lore.setContent("# Header\n[lore:123#section]Link[/lore]\nContent");
                    lore.setAccount(creatorAccount);
                    Long loreId = loreRepository.save(lore).getId();

                    try {
                        mockMvc.perform(get("/api/v1/lore/{roomId}/{loreId}/viewInline", testRoom.getId(), loreId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title", equalTo("Inline Lore")))
                                .andExpect(jsonPath("$.excerpts", notNullValue()));

                        verify(loreViewService).setViewed(eq(testAccount.getId()), eq(loreId));
                    } finally {
                        clearAuthentication();
                    }
                }
            }

//        // ====== EDGE CASES ======
//        @Nested
//        @DisplayName("Edge cases")
//        class EdgeCasesTests {
//
//            @Test
//            @DisplayName("should handle empty request body fields")
//            void createLore_emptyFields() throws Exception {
//                authenticateAs(creatorProfile);
//
//                LoreRequest request = new LoreRequest("Title", "", null, null, null, null);
//
//                try {
//                    mockMvc.perform(post("/api/v1/lore/{roomId}", testRoom.getId())
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .content(objectMapper.writeValueAsString(request)))
//                            .andExpect(status().isCreated());
//                } finally {
//                    clearAuthentication();
//                }
//            }
//
//            @Test
//            @DisplayName("should handle pagination parameters")
//            void getLoreList_pagination() throws Exception {
//                authenticateAs(testProfile);
//
//                for (int i = 0; i < 15; i++) {
//                    Lore lore = new Lore();
//                    lore.setTitle("Lore " + i);
//                    lore.setAccount(creatorAccount);
//                    loreRepository.save(lore);
//                }
//
//                try {
//                    mockMvc.perform(get("/api/v1/lore/{roomId}/all", testRoom.getId())
//                                    .param("page", "1")
//                                    .param("size", "10"))
//                            .andExpect(status().isOk())
//                            .andExpect(jsonPath("$.content", hasSize(5)))
//                            .andExpect(jsonPath("$.number", equalTo(1)))
//                            .andExpect(jsonPath("$.size", equalTo(10)));
//                } finally {
//                    clearAuthentication();
//                }
//            }
        }
    }
}
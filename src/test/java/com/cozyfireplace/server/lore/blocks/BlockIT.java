package com.cozyfireplace.server.lore.blocks;

import com.cozyfireplace.server.BaseIntegrationTest;
import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.characters.Character;
import com.cozyfireplace.server.characters.CharacterRepository;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.LoreRepository;
import com.cozyfireplace.server.lore.blocks.dto.ChanceConfigRequest;
import com.cozyfireplace.server.rooms.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link BlockController} — {@code /api/v1/blocks}.
 *
 * <p>Runs the real MVC + Spring Security + JPA stack against the shared Testcontainers
 * PostgreSQL. Blocks have no dedicated create endpoint (they are produced as a side effect of
 * saving lore / character content), so the fixtures seed them straight through
 * {@link BlockRepository}. The interesting domain rule under test is the <b>room resolution</b>
 * done by the {@code coalesce(l.account.room.id, c.account.room.id)} predicates in
 * {@link BlockRepository#findBlockIdsByRoomId} and {@link BlockRepository#findAccessMetaByBlockId}:
 * a block owned by a <em>character</em> belongs to the room of that character's account, exactly
 * like a block owned by a lore entry. {@code GET /{roomId}/list} must therefore surface
 * character-owned blocks, and {@code GET /{roomId}/{blockId}/access} must resolve their room via
 * {@code block.character.account.room}.</p>
 *
 * <p><b>Documented status map</b> (consistent across the whole IT suite):
 * <ul>
 *   <li>anonymous request → 403 (every block endpoint is {@code authenticated()});</li>
 *   <li>an unknown block id on the <em>reads</em> ({@code getBlock} / {@code getBlockWrapped} /
 *       {@code getBlockAccess}) still raises a bare {@code jakarta.persistence.EntityNotFoundException}
 *       which {@code GlobalExceptionHandler} does not map → <b>500</b> (pinned);</li>
 *   <li>an unknown block id on the <em>mutations</em> ({@code roll} / {@code chance}) is looked up
 *       with the room-scoped {@code findByIdWithChanceAndRolesInRoom} and answers
 *       {@code NotFoundException} → <b>404</b>; the same query makes another room's block id a 404
 *       too, so the API never confirms it exists elsewhere;</li>
 *   <li>{@code BlockMapper.toDetailResponse} dereferences {@code block.getLore()}, so reading the
 *       <em>detail</em> of a <b>character-owned</b> block (whose {@code lore} is {@code null}) NPEs
 *       → 500 (pinned); the list summary and access endpoints handle it fine;</li>
 *   <li>an invalid {@code type} query param raises a plain {@code IllegalArgumentException} → 500
 *       (pinned);</li>
 *   <li>{@code roll} on a non-chance block throws {@code IllegalStateException} → 500 (pinned);</li>
 *   <li>{@code chance} and the {@code access} reset / grant / deny endpoints are creator-gated
 *       ({@code RoomSecurityService#isCreator(roomId)}) → a member who is not the creator gets 403,
 *       and a profile without an account in the room also gets 403 (mapped
 *       {@code AccessDeniedException} from the resolver).</li>
 * </ul>
 */
@DisplayName("BlockIT — /api/v1/blocks")
class BlockIT extends BaseIntegrationTest {

    @Autowired
    BlockRepository blockRepository;
    @Autowired
    LoreRepository loreRepository;
    @Autowired
    CharacterRepository characterRepository;

    Profile creatorProfile;
    Room room;
    Account creatorAccount;
    Profile memberProfile;
    Account memberAccount;

    @BeforeEach
    void seedRoom() {
        creatorProfile = newProfile("dm");
        room = newRoomOwnedBy(creatorProfile, "Block Room");
        creatorAccount = room.getCreator();
        memberProfile = newProfile("player");
        memberAccount = newAccount(memberProfile, room, "Player One");
    }

    // ====== Fixture builders ======

    private Lore newLore(String title) {
        return loreRepository.save(Lore.builder()
                .title(title).content("Body of " + title).account(creatorAccount).build());
    }

    private Block loreBlock(Lore lore, String content) {
        return blockRepository.save(Block.builder()
                .lore(lore).content(content).roles(new HashSet<>()).build());
    }

    private Block chanceLoreBlock(Lore lore, String content) {
        Block block = Block.builder()
                .lore(lore).content(content).roles(new HashSet<>()).build();
        // ChanceConfig owns the FK (block_id); the skill column stays null, which is irrelevant
        // for the list / access surfaces exercised here.
        block.setChanceConfig(ChanceConfig.builder().block(block).threshold(10).build());
        return blockRepository.save(block);
    }

    private Character newCharacter(Account owner, String name) {
        return characterRepository.save(Character.builder()
                .name(name).content("Sheet of " + name).account(owner).roles(new HashSet<>()).build());
    }

    private Block characterBlock(Character character, String content) {
        return blockRepository.save(Block.builder()
                .character(character).content(content).roles(new HashSet<>()).build());
    }

    @Nested
    @DisplayName("GET /api/v1/blocks/{roomId}/list")
    class ListTests {

        @Test
        @DisplayName("lists both lore-owned and character-owned blocks of the room")
        void list_includesCharacterOwned() throws Exception {
            Lore lore = newLore("Gate Lore");
            Block loreBlk = loreBlock(lore, "rune on the gate");
            Character ch = newCharacter(memberAccount, "Aria");
            Block charBlk = characterBlock(ch, "hidden dagger trick");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/list", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.id == " + loreBlk.getId() + ")]").exists())
                    .andExpect(jsonPath("$.content[?(@.id == " + charBlk.getId() + ")]").exists())
                    .andExpect(jsonPath("$.page.totalElements").value(2));
        }

        @Test
        @DisplayName("character-owned block summary has no loreId and is typed 'normal'")
        void list_characterSummaryShape() throws Exception {
            Character ch = newCharacter(memberAccount, "Bram");
            Block charBlk = characterBlock(ch, "poison brew");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/list", room.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(charBlk.getId()))
                    .andExpect(jsonPath("$.content[0].loreId").value(nullValue()))
                    .andExpect(jsonPath("$.content[0].type").value("normal"));
        }

        @Test
        @DisplayName("type=chance keeps only chance blocks; type=normal keeps the rest")
        void list_typeFilter() throws Exception {
            Lore lore = newLore("Altar");
            Block normal = loreBlock(lore, "carved stone");
            Block chance = chanceLoreBlock(lore, "ward trigger");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/list", room.getId()).param("type", "chance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.id == " + chance.getId() + ")]").exists())
                    .andExpect(jsonPath("$.content[?(@.id == " + normal.getId() + ")]").doesNotExist());

            mockMvc.perform(get("/api/v1/blocks/{roomId}/list", room.getId()).param("type", "normal"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.id == " + normal.getId() + ")]").exists())
                    .andExpect(jsonPath("$.content[?(@.id == " + chance.getId() + ")]").doesNotExist());
        }

        @Test
        @DisplayName("search matches the owning lore title (coalesce of lore/character name)")
        void list_searchFilter() throws Exception {
            Lore match = newLore("Ancient Rune");
            Lore other = newLore("Blacksmith");
            Block blk = loreBlock(match, "glowing glyph");
            loreBlock(other, "hammer");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/list", room.getId()).param("search", "rune"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(blk.getId()));
        }

        @Test
        @DisplayName("id filter narrows to a single owning lore")
        void list_loreIdFilter() throws Exception {
            Lore one = newLore("One");
            Lore two = newLore("Two");
            loreBlock(one, "a");
            loreBlock(two, "b");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/list", room.getId())
                            .param("id", String.valueOf(two.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].loreId").value(two.getId()));
        }

        @Test
        @DisplayName("invalid type value → 500 (unmapped IllegalArgumentException)")
        void list_badType_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/list", room.getId()).param("type", "magic"))
                    // Pinned: parseBlockType throws a bare IllegalArgumentException with no handler → 500.
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("non-numeric id filter → 500 (unmapped IllegalArgumentException)")
        void list_badId_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/list", room.getId()).param("id", "abc"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/blocks/{roomId}/{blockId} — detail")
    class DetailTests {

        @Test
        @DisplayName("lore-owned normal block returns its detail (200)")
        void detail_loreOwned_success() throws Exception {
            Lore lore = newLore("Detail Lore");
            Block blk = loreBlock(lore, "the secret");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/{blockId}", room.getId(), blk.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(blk.getId()))
                    .andExpect(jsonPath("$.loreId").value(lore.getId()))
                    .andExpect(jsonPath("$.title").value("Detail Lore"))
                    .andExpect(jsonPath("$.content").value("the secret"))
                    .andExpect(jsonPath("$.type").value("normal"));
        }

        @Test
        @DisplayName("character-owned block detail → 500 (toDetailResponse NPEs on null lore)")
        void detail_characterOwned_returns500_currentBehaviour() throws Exception {
            Character ch = newCharacter(memberAccount, "Nyx");
            Block blk = characterBlock(ch, "shadow step");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/{blockId}", room.getId(), blk.getId()))
                    // Pinned: mapping a character-owned detail dereferences a null lore → NPE → 500.
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("unknown block id → 500 (unmapped EntityNotFoundException)")
        void detail_unknown_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/{blockId}", room.getId(), 9_999_999L))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/blocks/{roomId}/{blockId}/wrapped")
    class WrappedTests {

        @Test
        @DisplayName("lore-owned normal block returns the expanded wrapper")
        void wrapped_success() throws Exception {
            Lore lore = newLore("Wrap Lore");
            Block blk = loreBlock(lore, "wrapped body");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/{blockId}/wrapped", room.getId(), blk.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").exists());
        }

        @Test
        @DisplayName("unknown block id → 500 (unmapped EntityNotFoundException)")
        void wrapped_unknown_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/{blockId}/wrapped", room.getId(), 9_999_999L))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/blocks/{roomId}/{blockId}/access")
    class AccessTests {

        @Test
        @DisplayName("lore-owned normal block resolves as NORMAL access")
        void access_loreOwnedNormal() throws Exception {
            Lore lore = newLore("Access Lore");
            Block blk = loreBlock(lore, "restricted note");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/{blockId}/access", room.getId(), blk.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessType").value("NORMAL"))
                    .andExpect(jsonPath("$.records").isArray());
        }

        @Test
        @DisplayName("chance block resolves as CHANCE access")
        void access_chanceBlock() throws Exception {
            Lore lore = newLore("Ward Lore");
            Block blk = chanceLoreBlock(lore, "ward");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/{blockId}/access", room.getId(), blk.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessType").value("CHANCE"));
        }

        @Test
        @DisplayName("character-owned block access resolves via character.account.room")
        void access_characterOwned_resolvesRoomViaCharacter() throws Exception {
            Character ch = newCharacter(memberAccount, "Cinder");
            Block blk = characterBlock(ch, "ember trick");

            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/{blockId}/access", room.getId(), blk.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessType").value("NORMAL"));
        }

        @Test
        @DisplayName("unknown block id → 500 (unmapped EntityNotFoundException)")
        void access_unknown_returns500_currentBehaviour() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(get("/api/v1/blocks/{roomId}/{blockId}/access", room.getId(), 9_999_999L))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/blocks/{roomId}/{blockId}/roll")
    class RollTests {

        @Test
        @DisplayName("rolling a non-chance block → 500 (IllegalStateException is unmapped)")
        void roll_normalBlock_returns500_currentBehaviour() throws Exception {
            Lore lore = newLore("Plain Lore");
            Block blk = loreBlock(lore, "no roll here");

            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/blocks/{roomId}/{blockId}/roll", room.getId(), blk.getId()))
                    // Pinned: roll() refuses a block without a chance config via a bare IllegalStateException.
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("unknown block id → 404 (roll looks the block up inside the path room)")
        void roll_unknown_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/blocks/{roomId}/{blockId}/roll", room.getId(), 9_999_999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/blocks/{roomId}/access/{recordId}/{action}")
    class AccessMutationTests {

        @Test
        @DisplayName("unknown access record id → 404 (record resolved inside the path room)")
        void reset_unknownRecord_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/blocks/{roomId}/access/{recordId}/reset", room.getId(), 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("grant refuses an unknown record id → 404")
        void grant_unknownRecord_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/blocks/{roomId}/access/{recordId}/grant", room.getId(), 9_999_999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deny refuses an unknown record id → 404")
        void deny_unknownRecord_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(post("/api/v1/blocks/{roomId}/access/{recordId}/deny", room.getId(), 9_999_999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/blocks/{roomId}/{blockId}/chance")
    class ChanceConfigTests {

        @Test
        @DisplayName("unknown block id → 404 (block resolved inside the path room)")
        void chance_unknownBlock_returns404() throws Exception {
            authenticateAs(creatorProfile);
            mockMvc.perform(patch("/api/v1/blocks/{roomId}/{blockId}/chance", room.getId(), 9_999_999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new ChanceConfigRequest("perception", 15))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("existing block but unknown skill id → 404 (skill lookup refuses missing keys)")
        void chance_unknownSkill_returns404() throws Exception {
            Lore lore = newLore("Skill Lore");
            Block blk = loreBlock(lore, "will gain a chance config");

            authenticateAs(creatorProfile);
            mockMvc.perform(patch("/api/v1/blocks/{roomId}/{blockId}/chance", room.getId(), blk.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new ChanceConfigRequest("no-such-skill", 15))))
                    .andExpect(status().isNotFound());

            assertThat(blockRepository.findById(blk.getId()).orElseThrow().getChanceConfig()).isNull();
        }

        @Test
        @DisplayName("member who is not the room creator → 403 (chance config is creator-only)")
        void chance_byMember_returns403() throws Exception {
            Lore lore = newLore("Locked Lore");
            Block blk = loreBlock(lore, "not yours to tune");

            authenticateAs(memberProfile);
            mockMvc.perform(patch("/api/v1/blocks/{roomId}/{blockId}/chance", room.getId(), blk.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new ChanceConfigRequest("perception", 15))))
                    .andExpect(status().isForbidden());

            assertThat(blockRepository.findById(blk.getId()).orElseThrow().getChanceConfig()).isNull();
        }
    }

    @Nested
    @DisplayName("Security")
    class SecurityTests {

        @Test
        @DisplayName("anonymous list request → 403")
        void anonymous_returns403() throws Exception {
            clearAuthentication();
            mockMvc.perform(get("/api/v1/blocks/{roomId}/list", room.getId()))
                    .andExpect(status().isForbidden());
        }
    }
}

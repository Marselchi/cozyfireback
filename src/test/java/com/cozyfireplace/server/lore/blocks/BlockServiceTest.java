package com.cozyfireplace.server.lore.blocks;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.AccountChar;
import com.cozyfireplace.server.accounts.AccountRepository;
import com.cozyfireplace.server.accounts.stats.AccountCharSkill;
import com.cozyfireplace.server.accounts.stats.Skill;
import com.cozyfireplace.server.accounts.stats.SkillRepository;
import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.lore.blocks.dto.*;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.roles.RoleRepository;
import com.cozyfireplace.server.rooms.RoomSecurityService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure Mockito unit tests for {@link BlockService}.
 * <p>
 * Entities ({@link Block}, {@link ChanceConfig}, {@link AccessRecord}) are used
 * as real objects; only repositories, the mapper, the security service and the
 * account store are mocked. A real {@link ObjectMapper} is injected so the
 * serialized block metadata in expanded blocks is asserted byte-for-byte.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BlockService")
class BlockServiceTest {

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private AccessRecordRepository accessRecordRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private BlockMapper blockMapper;
    @Mock
    private RoomSecurityService roomSecurityService;
    @Mock
    private AccountRepository accountRepository;
    @Spy
    @SuppressWarnings("unused")
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BlockService blockService;

    // ================= helpers =================

    private Account account(Long charId, Role... roles) {
        Account account = mock(Account.class);
        lenient().when(account.getId()).thenReturn(7L);
        lenient().when(account.getRoles()).thenReturn(Set.of(roles));
        if (charId != null) {
            AccountChar ch = mock(AccountChar.class);
            lenient().when(ch.getId()).thenReturn(charId);
            lenient().when(ch.getSkills()).thenReturn(Set.of());
            lenient().when(account.getAccountChar()).thenReturn(ch);
        }
        return account;
    }

    private Role role(long id) {
        Role role = mock(Role.class);
        lenient().when(role.getId()).thenReturn(id);
        return role;
    }

    private Block block(long id, String content, ChanceConfig chanceConfig, Set<Role> roles) {
        return Block.builder()
                .id(id)
                .content(content)
                .chanceConfig(chanceConfig)
                .roles(roles)
                .build();
    }

    private static String placeholder(long blockId) {
        return "{restrictedBlock id=\"" + blockId + "\"}";
    }

    private static String localPlaceholder(String localId) {
        return "{restrictedBlock localId=\"" + localId + "\"}";
    }

    // ================= processBlockChanges =================

    @Nested
    @DisplayName("processBlockChanges")
    class ProcessBlockChanges {

        @Test
        @DisplayName("deletes, updates and creates blocks and swaps localId placeholders for real ids")
        void fullCycle() {
            Lore lore = Lore.builder().id(1L).content("intro " + localPlaceholder("abc") + " tail").build();
            Block existing = block(9L, "old", null, new HashSet<>());

            UpdatedBlockRequest update = UpdatedBlockRequest.builder()
                    .id("9").content("updated").metadata(null).build();
            AddedBlockRequest add = AddedBlockRequest.builder()
                    .localId("abc").content("new block").metadata(null).build();
            BlockChangesRequest request = BlockChangesRequest.builder()
                    .deletedBlockIds(List.of("1", "2"))
                    .updatedBlocks(List.of(update))
                    .addedBlocks(List.of(add))
                    .build();

            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(existing));
            when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
                Block b = inv.getArgument(0);
                if (b.getId() == null) {
                    b.setId(55L); // freshly created block gets a generated id
                }
                return b;
            });

            String result = blockService.processBlockChanges(request, lore);

            verify(blockRepository).deleteAllById(List.of(1L, 2L));
            assertEquals("updated", existing.getContent());
            assertEquals("intro " + placeholder(55) + " tail", result);
        }

        @Test
        @DisplayName("null change lists leave the content untouched")
        void noChanges() {
            Lore lore = Lore.builder().content("plain content").build();
            BlockChangesRequest request = new BlockChangesRequest();

            String result = blockService.processBlockChanges(request, lore);

            assertEquals("plain content", result);
            verifyNoInteractions(blockRepository);
        }

        @Test
        @DisplayName("roles from metadata are resolved through the role repository on create")
        void createWithRoles() {
            Lore lore = Lore.builder().content(localPlaceholder("x")).build();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("roles", List.of("3"));
            AddedBlockRequest add = AddedBlockRequest.builder()
                    .localId("x").content("c").metadata(metadata).build();
            BlockChangesRequest request = BlockChangesRequest.builder()
                    .addedBlocks(List.of(add)).build();

            Role r3 = role(3);
            when(roleRepository.findAllById(List.of(3L))).thenReturn(List.of(r3));
            when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
                ((Block) inv.getArgument(0)).setId(12L);
                return inv.getArgument(0);
            });

            String result = blockService.processBlockChanges(request, lore);

            ArgumentCaptor<Block> captor = ArgumentCaptor.forClass(Block.class);
            verify(blockRepository).save(captor.capture());
            assertEquals(Set.of(r3), captor.getValue().getRoles());
            assertEquals(placeholder(12), result);
        }

        @Test
        @DisplayName("chance spoilerType on create builds a ChanceConfig from metadata")
        void createWithChanceMetadata() {
            Lore lore = Lore.builder().content(localPlaceholder("x")).build();
            Map<String, Object> chance = new HashMap<>();
            chance.put("skill", "perception");
            chance.put("threshold", "15");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("spoilerType", "chance");
            metadata.put("chance", chance);
            AddedBlockRequest add = AddedBlockRequest.builder()
                    .localId("x").content("c").metadata(metadata).build();
            BlockChangesRequest request = BlockChangesRequest.builder()
                    .addedBlocks(List.of(add)).build();

            Skill skill = Skill.builder().key("perception").label("Perception").build();
            when(skillRepository.findById("perception")).thenReturn(Optional.of(skill));
            when(blockRepository.save(any(Block.class))).thenAnswer(inv -> {
                ((Block) inv.getArgument(0)).setId(13L);
                return inv.getArgument(0);
            });

            blockService.processBlockChanges(request, lore);

            ArgumentCaptor<Block> captor = ArgumentCaptor.forClass(Block.class);
            verify(blockRepository).save(captor.capture());
            ChanceConfig config = captor.getValue().getChanceConfig();
            assertNotNull(config);
            assertSame(skill, config.getSkill());
            assertEquals(15, config.getThreshold());
        }

        @Test
        @DisplayName("update with non-chance spoilerType drops an existing ChanceConfig")
        void updateDropsChanceConfig() {
            Lore lore = Lore.builder().content("whatever").build();
            ChanceConfig config = ChanceConfig.builder().id(1L).threshold(5).build();
            Block existing = block(9L, "old", config, new HashSet<>());
            existing.getChanceConfig().setBlock(existing);
            UpdatedBlockRequest update = UpdatedBlockRequest.builder()
                    .id("9").content("new").metadata(Map.of("spoilerType", "normal")).build();
            BlockChangesRequest request = BlockChangesRequest.builder()
                    .updatedBlocks(List.of(update)).build();

            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(existing));
            when(blockRepository.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            blockService.processBlockChanges(request, lore);

            assertNull(existing.getChanceConfig());
        }

        @Test
        @DisplayName("update of a missing block throws EntityNotFoundException")
        void updateMissingBlock() {
            Lore lore = Lore.builder().content("c").build();
            UpdatedBlockRequest update = UpdatedBlockRequest.builder().id("99").content("x").build();
            BlockChangesRequest request = BlockChangesRequest.builder()
                    .updatedBlocks(List.of(update)).build();
            when(blockRepository.findByIdWithChanceAndRoles(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> blockService.processBlockChanges(request, lore));
        }

        @Test
        @DisplayName("update with unknown skill key throws EntityNotFoundException")
        void updateChanceUnknownSkill() {
            Lore lore = Lore.builder().content("c").build();
            Block existing = block(9L, "old", null, new HashSet<>());
            Map<String, Object> chance = new HashMap<>();
            chance.put("skill", "athletics");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("spoilerType", "chance");
            metadata.put("chance", chance);
            UpdatedBlockRequest update = UpdatedBlockRequest.builder()
                    .id("9").content("x").metadata(metadata).build();
            BlockChangesRequest request = BlockChangesRequest.builder()
                    .updatedBlocks(List.of(update)).build();

            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(existing));
            when(skillRepository.findById("athletics")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> blockService.processBlockChanges(request, lore));
        }
    }

    // ================= getBlocksByRoomId =================

    @Nested
    @DisplayName("getBlocksByRoomId")
    class GetBlocksByRoomId {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("returns an empty page when no ids are found")
        void emptyResult() {
            when(blockRepository.findBlockIdsByRoomId(1L, null, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<BlockSummaryResponse> page = blockService.getBlocksByRoomId(1L, null, null, null, pageable);

            assertTrue(page.isEmpty());
            verify(blockRepository, never()).findBlocksForSummaryByIds(anyList());
        }

        @Test
        @DisplayName("maps blocks to summaries preserving id order and dropping missing rows")
        void populatedResult() {
            Block b1 = block(1L, "one", null, new HashSet<>());
            when(blockRepository.findBlockIdsByRoomId(1L, null, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(1L, 2L), pageable, 2));
            when(blockRepository.findBlocksForSummaryByIds(List.of(1L, 2L))).thenReturn(List.of(b1));
            when(blockMapper.toSummaryResponse(b1)).thenReturn(
                    BlockSummaryResponse.builder().id(1L).build());

            Page<BlockSummaryResponse> page = blockService.getBlocksByRoomId(1L, null, null, null, pageable);

            assertEquals(1, page.getContent().size());
            assertEquals(1L, page.getContent().getFirst().getId());
        }

        @Test
        @DisplayName("numeric loreId param is forwarded, search is trimmed")
        void paramsTranslated() {
            when(blockRepository.findBlockIdsByRoomId(1L, 42L, "ancient", true, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            blockService.getBlocksByRoomId(1L, " 42 ", " ancient ", "chance", pageable);

            verify(blockRepository).findBlockIdsByRoomId(1L, 42L, "ancient", true, pageable);
        }

        @Test
        @DisplayName("blank search and type become nulls")
        void blankParams() {
            when(blockRepository.findBlockIdsByRoomId(1L, null, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            blockService.getBlocksByRoomId(1L, "  ", "   ", " ", pageable);

            verify(blockRepository).findBlockIdsByRoomId(1L, null, null, null, pageable);
        }

        @Test
        @DisplayName("non-numeric loreId param throws IllegalArgumentException")
        void invalidLoreId() {
            assertThrows(IllegalArgumentException.class,
                    () -> blockService.getBlocksByRoomId(1L, "abc", null, null, pageable));
        }

        @Test
        @DisplayName("unknown block type throws IllegalArgumentException")
        void invalidType() {
            assertThrows(IllegalArgumentException.class,
                    () -> blockService.getBlocksByRoomId(1L, null, null, "magic", pageable));
        }

        @Test
        @DisplayName("type 'normal' maps to hasChance=false")
        void normalType() {
            when(blockRepository.findBlockIdsByRoomId(1L, null, null, false, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            blockService.getBlocksByRoomId(1L, null, null, "NORMAL", pageable);

            verify(blockRepository).findBlockIdsByRoomId(1L, null, null, false, pageable);
        }
    }

    // ================= expandBlocks =================

    @Nested
    @DisplayName("expandBlocks")
    class ExpandBlocks {

        @Test
        @DisplayName("null or empty content is returned unchanged")
        void emptyContent() {
            Account acc = account(null);
            assertNull(blockService.expandBlocks(null, acc));
            assertEquals("", blockService.expandBlocks("", acc));
        }

        @Test
        @DisplayName("content without markers is returned unchanged without repository hits")
        void noMarkers() {
            Account acc = account(null);
            assertEquals("plain text", blockService.expandBlocks("plain text", acc));
            verifyNoInteractions(blockRepository);
        }

        @Test
        @DisplayName("marker of a deleted block is removed")
        void blockNotFoundRemoved() {
            Account acc = account(null);
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(5L))).thenReturn(List.of());

            String result = blockService.expandBlocks("a " + placeholder(5) + " b", acc);

            assertEquals("a  b", result);
        }

        @Test
        @DisplayName("normal block the viewer has no role for is hidden")
        void noRoleAccess() {
            Account acc = account(null, role(11));
            Block b = block(5L, "secret", null, Set.of(role(42)));
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(5L))).thenReturn(List.of(b));

            String result = blockService.expandBlocks(placeholder(5), acc);

            assertEquals("", result);
        }

        @Test
        @DisplayName("normal block without roles is expanded with full metadata header")
        void normalBlockExpanded() {
            Account acc = account(null);
            Block b = block(5L, "body text", null, Set.of());
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(5L))).thenReturn(List.of(b));

            String result = blockService.expandBlocks(placeholder(5), acc);

            assertEquals("""
                    ::: restrictedBlock {"id":"5","spoilerType":"normal","roles":[]}
                    body text
                    :::""", result);
        }

        @Test
        @DisplayName("block with a matching role is expanded for the viewer")
        void roleMatchExpands() {
            Role r11 = role(11);
            Account acc = account(null, r11);
            Block b = block(5L, "party secret", null, Set.of(r11));
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(5L))).thenReturn(List.of(b));

            String result = blockService.expandBlocks(placeholder(5), acc);

            assertTrue(result.contains("party secret"));
        }

        @Test
        @DisplayName("chance block without an access record becomes a rollable placeholder")
        void chanceWithoutRecordRollable() {
            Account acc = account(500L);
            Skill skill = Skill.builder().key("perception").label("Perception").build();
            ChanceConfig config = ChanceConfig.builder().id(1L).threshold(15).skill(skill).build();
            Block b = block(7L, "chance body", config, Set.of());
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(7L))).thenReturn(List.of(b));
            when(accessRecordRepository.findAllByBlockIdsAndAccountCharId(List.of(7L), 500L))
                    .thenReturn(List.of());

            String result = blockService.expandBlocks(placeholder(7), acc);

            assertEquals("{rollable id=\"7\"}", result);
        }

        @Test
        @DisplayName("chance block with a PASSED record is fully expanded (with chance metadata)")
        void chancePassedExpands() {
            Account acc = account(500L);
            Skill skill = Skill.builder().key("perception").label("Perception").build();
            ChanceConfig config = ChanceConfig.builder().id(1L).threshold(15).skill(skill).build();
            Block b = block(7L, "revealed", config, Set.of());
            AccessRecord record = AccessRecord.builder()
                    .id(3L).block(b).status(AccessStatus.PASSED).rollValue(18).build();
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(7L))).thenReturn(List.of(b));
            when(accessRecordRepository.findAllByBlockIdsAndAccountCharId(List.of(7L), 500L))
                    .thenReturn(List.of(record));

            String result = blockService.expandBlocks(placeholder(7), acc);

            assertEquals("""
                    ::: restrictedBlock {"id":"7","spoilerType":"chance","roles":[],"chance":{"skill":"perception","threshold":15}}
                    revealed
                    :::""", result);
        }

        @Test
        @DisplayName("chance block with a DENIED record stays hidden")
        void chanceDeniedHidden() {
            Account acc = account(500L);
            Skill skill = Skill.builder().key("perception").label("Perception").build();
            ChanceConfig config = ChanceConfig.builder().id(1L).threshold(15).skill(skill).build();
            Block b = block(7L, "hidden", config, Set.of());
            AccessRecord record = AccessRecord.builder()
                    .id(3L).block(b).status(AccessStatus.DENIED).build();
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(7L))).thenReturn(List.of(b));
            when(accessRecordRepository.findAllByBlockIdsAndAccountCharId(List.of(7L), 500L))
                    .thenReturn(List.of(record));

            assertEquals("", blockService.expandBlocks(placeholder(7), acc));
        }

        @Test
        @DisplayName("chance block with a FAIL record stays hidden")
        void chanceFailHidden() {
            Account acc = account(500L);
            Skill skill = Skill.builder().key("perception").label("Perception").build();
            ChanceConfig config = ChanceConfig.builder().id(1L).threshold(15).skill(skill).build();
            Block b = block(7L, "hidden", config, Set.of());
            AccessRecord record = AccessRecord.builder()
                    .id(3L).block(b).status(AccessStatus.FAIL).rollValue(2).build();
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(7L))).thenReturn(List.of(b));
            when(accessRecordRepository.findAllByBlockIdsAndAccountCharId(List.of(7L), 500L))
                    .thenReturn(List.of(record));

            assertEquals("", blockService.expandBlocks(placeholder(7), acc));
        }

        @Test
        @DisplayName("creator (canSeeAll) sees every block expanded, even restricted chance ones")
        void creatorSeesAll() {
            Account acc = account(500L);
            Skill skill = Skill.builder().key("perception").label("Perception").build();
            ChanceConfig config = ChanceConfig.builder().id(1L).threshold(15).skill(skill).build();
            Block b = block(7L, "dm secret", config, Set.of(role(99)));
            when(roomSecurityService.isCreator(acc)).thenReturn(true);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(7L))).thenReturn(List.of(b));
            when(accessRecordRepository.findAllByBlockIdsAndAccountCharId(List.of(7L), 500L))
                    .thenReturn(List.of());

            String result = blockService.expandBlocks(placeholder(7), acc);

            assertTrue(result.contains("dm secret"));
        }

        @Test
        @DisplayName("account without a character never loads access records")
        void noCharacterNoRecordLookup() {
            Account acc = account(null);
            Skill skill = Skill.builder().key("perception").label("Perception").build();
            ChanceConfig config = ChanceConfig.builder().id(1L).threshold(15).skill(skill).build();
            Block b = block(7L, "chance body", config, Set.of());
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(7L))).thenReturn(List.of(b));

            String result = blockService.expandBlocks(placeholder(7), acc);

            verify(accessRecordRepository, never()).findAllByBlockIdsAndAccountCharId(anyList(), anyLong());
            // No record can exist -> rollable
            assertEquals("{rollable id=\"7\"}", result);
        }

        @Test
        @DisplayName("surrounding public text is preserved around expanded blocks")
        void publicTextPreserved() {
            Account acc = account(null);
            Block b = block(5L, "mid", null, Set.of());
            when(roomSecurityService.isCreator(acc)).thenReturn(false);
            when(blockRepository.findAllWithChanceAndRolesByIds(List.of(5L))).thenReturn(List.of(b));

            String result = blockService.expandBlocks("before " + placeholder(5) + " after", acc);

            assertTrue(result.startsWith("before ::: restrictedBlock"));
            assertTrue(result.endsWith("\n::: after"));
        }
    }

    // ================= getBlockAccess =================

    @Nested
    @DisplayName("getBlockAccess")
    class GetBlockAccess {

        private BlockAccessMeta meta(Long roomId, Boolean hasChance) {
            return new BlockAccessMeta() {
                @Override
                public Long getRoomId() {
                    return roomId;
                }

                @Override
                public Boolean getHasChance() {
                    return hasChance;
                }
            };
        }

        @Test
        @DisplayName("chance block loads chance access records")
        void chanceBlock() {
            List<AccessRecordResponse> records = List.of(AccessRecordResponse.builder().id(1L).build());
            when(blockRepository.findAccessMetaByBlockId(9L)).thenReturn(Optional.of(meta(1L, true)));
            when(accessRecordRepository.findChanceAccess(1L, 9L)).thenReturn(records);

            BlockAccessResponse<?> response = blockService.getBlockAccess(9L);

            assertEquals(AccessType.CHANCE, response.getAccessType());
            assertSame(records, response.getRecords());
        }

        @Test
        @DisplayName("normal block loads normal access from the account repository")
        void normalBlock() {
            List<NormalAccessRecordResponse> records = List.of(mock(NormalAccessRecordResponse.class));
            when(blockRepository.findAccessMetaByBlockId(9L)).thenReturn(Optional.of(meta(1L, false)));
            when(accountRepository.findNormalAccess(1L, 9L)).thenReturn(records);

            BlockAccessResponse<?> response = blockService.getBlockAccess(9L);

            assertEquals(AccessType.NORMAL, response.getAccessType());
            assertSame(records, response.getRecords());
        }

        @Test
        @DisplayName("unknown block throws EntityNotFoundException")
        void blockNotFound() {
            when(blockRepository.findAccessMetaByBlockId(9L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> blockService.getBlockAccess(9L));
        }

        @Test
        @DisplayName("block without a room throws EntityNotFoundException")
        void blockWithoutRoom() {
            when(blockRepository.findAccessMetaByBlockId(9L)).thenReturn(Optional.of(meta(null, false)));
            assertThrows(EntityNotFoundException.class, () -> blockService.getBlockAccess(9L));
        }
    }

    // ================= getBlock / getBlockWrapped =================

    @Nested
    @DisplayName("getBlock and getBlockWrapped")
    class GetSingleBlock {

        @Test
        @DisplayName("getBlock delegates to the mapper")
        void getBlock() {
            Lore lore = Lore.builder().id(2L).title("L").build();
            Block b = block(9L, "c", null, Set.of());
            b.setLore(lore);
            BlockDetailResponse detail = mock(BlockDetailResponse.class);
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(b));
            when(blockMapper.toDetailResponse(b, lore)).thenReturn(detail);

            assertSame(detail, blockService.getBlock(9L));
        }

        @Test
        @DisplayName("getBlock throws EntityNotFoundException for unknown id")
        void getBlockNotFound() {
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> blockService.getBlock(9L));
        }

        @Test
        @DisplayName("getBlockWrapped returns the fully expanded block markup")
        void getBlockWrapped() {
            Block b = block(9L, "wrapped body", null, Set.of(role(3)));
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(b));

            WrappedBlockResponse response = blockService.getBlockWrapped(9L);

            assertEquals("""
                    ::: restrictedBlock {"id":"9","spoilerType":"normal","roles":["3"]}
                    wrapped body
                    :::""", response.getContent());
        }

        @Test
        @DisplayName("getBlockWrapped throws EntityNotFoundException for unknown id")
        void getBlockWrappedNotFound() {
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> blockService.getBlockWrapped(9L));
        }
    }

    // ================= roll =================

    @Nested
    @DisplayName("roll")
    class Roll {

        private final Skill skill = Skill.builder().key("perception").label("Perception").build();

        private Block chanceBlock(int threshold) {
            ChanceConfig config = ChanceConfig.builder().id(1L).threshold(threshold).skill(skill).build();
            return block(9L, "chance", config, Set.of());
        }

        private Account accountWithSkillModifier(int modifier) {
            Account account = mock(Account.class);
            AccountChar ch = mock(AccountChar.class);
            lenient().when(ch.getId()).thenReturn(500L);
            AccountCharSkill charSkill = mock(AccountCharSkill.class);
            lenient().when(charSkill.getSkill()).thenReturn(skill);
            lenient().when(charSkill.getModifier()).thenReturn(modifier);
            lenient().when(ch.getSkills()).thenReturn(Set.of(charSkill));
            lenient().when(account.getAccountChar()).thenReturn(ch);
            return account;
        }

        @Test
        @DisplayName("unknown block throws EntityNotFoundException")
        void blockNotFound() {
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class,
                    () -> blockService.roll(9L, account(null)));
        }

        @Test
        @DisplayName("rolling a block without chance config throws IllegalStateException")
        void notAChanceBlock() {
            when(blockRepository.findByIdWithChanceAndRoles(9L))
                    .thenReturn(Optional.of(block(9L, "c", null, Set.of())));
            assertThrows(IllegalStateException.class,
                    () -> blockService.roll(9L, account(500L)));
        }

        @Test
        @DisplayName("account without a character cannot roll")
        void noCharacter() {
            when(blockRepository.findByIdWithChanceAndRoles(9L))
                    .thenReturn(Optional.of(chanceBlock(15)));
            assertThrows(IllegalStateException.class,
                    () -> blockService.roll(9L, account(null)));
        }

        @Test
        @DisplayName("an already resolved record cannot roll again")
        void alreadyRolled() {
            when(blockRepository.findByIdWithChanceAndRoles(9L))
                    .thenReturn(Optional.of(chanceBlock(15)));
            AccessRecord existing = AccessRecord.builder()
                    .id(1L).status(AccessStatus.PASSED).build();
            when(accessRecordRepository.findByBlockIdAndAccountCharId(9L, 500L))
                    .thenReturn(Optional.of(existing));

            assertThrows(IllegalStateException.class,
                    () -> blockService.roll(9L, accountWithSkillModifier(0)));
        }

        @Test
        @DisplayName("a PENDING record may roll again and is updated in place")
        void pendingRecordRerolls() {
            Block b = chanceBlock(0); // threshold 0 -> every d20 roll passes
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(b));
            AccessRecord existing = AccessRecord.builder()
                    .id(1L).block(b).status(AccessStatus.PENDING).build();
            when(accessRecordRepository.findByBlockIdAndAccountCharId(9L, 500L))
                    .thenReturn(Optional.of(existing));
            when(accessRecordRepository.save(any(AccessRecord.class))).thenAnswer(inv -> inv.getArgument(0));
            RollResultResponse result = mock(RollResultResponse.class);
            when(blockMapper.toRollResultResponse(existing, 0, "Perception"))
                    .thenReturn(result);

            assertSame(result, blockService.roll(9L, accountWithSkillModifier(0)));
            assertEquals(AccessStatus.PASSED, existing.getStatus());
            assertNotNull(existing.getRollValue());
        }

        @Test
        @DisplayName("a roll below the impossible-free threshold always fails; skill modifier is stored")
        void guaranteedFailStoresFailStatus() {
            Block b = chanceBlock(1000); // d20 + modifier can never reach 1000
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(b));
            when(accessRecordRepository.findByBlockIdAndAccountCharId(9L, 500L))
                    .thenReturn(Optional.empty());
            when(accessRecordRepository.save(any(AccessRecord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(blockMapper.toRollResultResponse(any(), eq(1000), eq("Perception")))
                    .thenReturn(mock(RollResultResponse.class));

            blockService.roll(9L, accountWithSkillModifier(3));

            ArgumentCaptor<AccessRecord> captor = ArgumentCaptor.forClass(AccessRecord.class);
            verify(accessRecordRepository).save(captor.capture());
            AccessRecord saved = captor.getValue();
            assertEquals(AccessStatus.FAIL, saved.getStatus());
            assertEquals(3, saved.getModifier());
            assertTrue(saved.getRollValue() >= 1 && saved.getRollValue() <= 20);
        }

        @Test
        @DisplayName("threshold 0 always passes")
        void guaranteedPassStoresPassedStatus() {
            Block b = chanceBlock(0);
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(b));
            when(accessRecordRepository.findByBlockIdAndAccountCharId(9L, 500L))
                    .thenReturn(Optional.empty());
            when(accessRecordRepository.save(any(AccessRecord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(blockMapper.toRollResultResponse(any(), eq(0), eq("Perception")))
                    .thenReturn(mock(RollResultResponse.class));

            blockService.roll(9L, accountWithSkillModifier(0));

            ArgumentCaptor<AccessRecord> captor = ArgumentCaptor.forClass(AccessRecord.class);
            verify(accessRecordRepository).save(captor.capture());
            assertEquals(AccessStatus.PASSED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("missing skill on the character contributes a 0 modifier")
        void missingSkillDefaultsToZeroModifier() {
            Block b = chanceBlock(0);
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(b));
            when(accessRecordRepository.findByBlockIdAndAccountCharId(9L, 500L))
                    .thenReturn(Optional.empty());
            when(accessRecordRepository.save(any(AccessRecord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(blockMapper.toRollResultResponse(any(), eq(0), eq("Perception")))
                    .thenReturn(mock(RollResultResponse.class));

            Account noSkills = account(500L); // account char exists but has no skills
            blockService.roll(9L, noSkills);

            ArgumentCaptor<AccessRecord> captor = ArgumentCaptor.forClass(AccessRecord.class);
            verify(accessRecordRepository).save(captor.capture());
            assertEquals(0, captor.getValue().getModifier());
        }
    }

    // ================= access record mutations =================

    @Nested
    @DisplayName("resetAccess / grantAccess / denyAccess")
    class AccessMutations {

        private AccessRecordWithAccount recordHolder(AccessRecord record) {
            return new AccessRecordWithAccount() {
                @Override
                public AccessRecord getRecord() {
                    return record;
                }

                @Override
                public Long getUserId() {
                    return 42L;
                }

                @Override
                public String getUserName() {
                    return "Player";
                }
            };
        }

        @Test
        @DisplayName("reset sets PENDING, clears the roll value and maps the response")
        void reset() {
            AccessRecord record = AccessRecord.builder().id(5L).status(AccessStatus.PASSED).rollValue(19).build();
            when(accessRecordRepository.findByIdWithAccount(5L))
                    .thenReturn(Optional.of(recordHolder(record)));
            when(accessRecordRepository.save(record)).thenReturn(record);
            AccessRecordResponse response = mock(AccessRecordResponse.class);
            when(blockMapper.toAccessRecordResponse(record, 42L, "Player")).thenReturn(response);

            assertSame(response, blockService.resetAccess(5L));
            assertEquals(AccessStatus.PENDING, record.getStatus());
            assertNull(record.getRollValue());
        }

        @Test
        @DisplayName("grant sets GRANTED")
        void grant() {
            AccessRecord record = AccessRecord.builder().id(5L).status(AccessStatus.PENDING).build();
            when(accessRecordRepository.findByIdWithAccount(5L))
                    .thenReturn(Optional.of(recordHolder(record)));
            when(accessRecordRepository.save(record)).thenReturn(record);
            when(blockMapper.toAccessRecordResponse(record, 42L, "Player"))
                    .thenReturn(mock(AccessRecordResponse.class));

            blockService.grantAccess(5L);

            assertEquals(AccessStatus.GRANTED, record.getStatus());
        }

        @Test
        @DisplayName("deny sets DENIED")
        void deny() {
            AccessRecord record = AccessRecord.builder().id(5L).status(AccessStatus.PENDING).build();
            when(accessRecordRepository.findByIdWithAccount(5L))
                    .thenReturn(Optional.of(recordHolder(record)));
            when(accessRecordRepository.save(record)).thenReturn(record);
            when(blockMapper.toAccessRecordResponse(record, 42L, "Player"))
                    .thenReturn(mock(AccessRecordResponse.class));

            blockService.denyAccess(5L);

            assertEquals(AccessStatus.DENIED, record.getStatus());
        }

        @Test
        @DisplayName("unknown access record throws EntityNotFoundException (reset)")
        void resetNotFound() {
            when(accessRecordRepository.findByIdWithAccount(5L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> blockService.resetAccess(5L));
        }

        @Test
        @DisplayName("unknown access record throws EntityNotFoundException (grant)")
        void grantNotFound() {
            when(accessRecordRepository.findByIdWithAccount(5L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> blockService.grantAccess(5L));
        }

        @Test
        @DisplayName("unknown access record throws EntityNotFoundException (deny)")
        void denyNotFound() {
            when(accessRecordRepository.findByIdWithAccount(5L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> blockService.denyAccess(5L));
        }
    }

    // ================= updateChanceConfig =================

    @Nested
    @DisplayName("updateChanceConfig")
    class UpdateChanceConfig {

        @Test
        @DisplayName("existing config is updated in place")
        void existingConfig() {
            ChanceConfig config = ChanceConfig.builder().id(1L).threshold(5).build();
            Block b = block(9L, "c", config, Set.of());
            Skill skill = Skill.builder().key("stealth").label("Stealth").build();
            ChanceConfigResponse response = mock(ChanceConfigResponse.class);
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(b));
            when(skillRepository.findById("stealth")).thenReturn(Optional.of(skill));
            when(blockRepository.save(b)).thenReturn(b);
            when(blockMapper.toChanceConfigResponse(config)).thenReturn(response);

            ChanceConfigResponse result = blockService.updateChanceConfig(
                    9L, ChanceConfigRequest.builder().skill("stealth").threshold(17).build());

            assertSame(response, result);
            assertSame(skill, config.getSkill());
            assertEquals(17, config.getThreshold());
        }

        @Test
        @DisplayName("a normal block gains a fresh ChanceConfig")
        void createsMissingConfig() {
            Block b = block(9L, "c", null, Set.of());
            Skill skill = Skill.builder().key("stealth").label("Stealth").build();
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.of(b));
            when(skillRepository.findById("stealth")).thenReturn(Optional.of(skill));
            when(blockRepository.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));
            when(blockMapper.toChanceConfigResponse(any(ChanceConfig.class)))
                    .thenReturn(mock(ChanceConfigResponse.class));

            blockService.updateChanceConfig(
                    9L, ChanceConfigRequest.builder().skill("stealth").threshold(12).build());

            ArgumentCaptor<ChanceConfig> captor = ArgumentCaptor.forClass(ChanceConfig.class);
            verify(blockMapper).toChanceConfigResponse(captor.capture());
            assertEquals(12, captor.getValue().getThreshold());
            assertSame(skill, captor.getValue().getSkill());
            assertTrue(b.hasChance());
        }

        @Test
        @DisplayName("unknown block throws EntityNotFoundException")
        void blockNotFound() {
            when(blockRepository.findByIdWithChanceAndRoles(9L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> blockService.updateChanceConfig(
                    9L, ChanceConfigRequest.builder().skill("stealth").threshold(1).build()));
        }

        @Test
        @DisplayName("unknown skill throws EntityNotFoundException")
        void skillNotFound() {
            when(blockRepository.findByIdWithChanceAndRoles(9L))
                    .thenReturn(Optional.of(block(9L, "c", null, Set.of())));
            when(skillRepository.findById("nope")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> blockService.updateChanceConfig(
                    9L, ChanceConfigRequest.builder().skill("nope").threshold(1).build()));
        }
    }

    // ================= getAccountRoleIds =================

    @Test
    @DisplayName("getAccountRoleIds maps entity roles to their ids")
    void getAccountRoleIds() {
        assertEquals(Set.of(1L, 2L), blockService.getAccountRoleIds(account(null, role(1), role(2))));
    }
}

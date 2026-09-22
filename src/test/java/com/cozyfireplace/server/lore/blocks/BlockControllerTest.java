package com.cozyfireplace.server.lore.blocks;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.blocks.dto.AccessRecordResponse;
import com.cozyfireplace.server.lore.blocks.dto.BlockAccessResponse;
import com.cozyfireplace.server.lore.blocks.dto.BlockDetailResponse;
import com.cozyfireplace.server.lore.blocks.dto.BlockSummaryResponse;
import com.cozyfireplace.server.lore.blocks.dto.ChanceConfigRequest;
import com.cozyfireplace.server.lore.blocks.dto.ChanceConfigResponse;
import com.cozyfireplace.server.lore.blocks.dto.RollResultResponse;
import com.cozyfireplace.server.lore.blocks.dto.WrappedBlockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BlockController}.
 * <p>
 * Every endpoint is a pure delegation to {@link BlockService}; the tests lock
 * that contract in place: correct status code, identical body object and the
 * exact arguments forwarded to the service (path variables, raw query
 * parameters and the authenticated account).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BlockController")
class BlockControllerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long BLOCK_ID = 99L;
    private static final Long RECORD_ID = 31L;

    @Mock
    private BlockService blockService;

    @InjectMocks
    private BlockController controller;

    // ================= helpers =================

    private Account account() {
        return mock(Account.class);
    }

    // ================= list =================

    @Test
    @DisplayName("GET /{roomId}/list forwards the raw lore-id, search and type params plus pageable")
    @SuppressWarnings("unchecked")
    void getBlocks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlockSummaryResponse> page = mock(Page.class);
        when(blockService.getBlocksByRoomId(ROOM_ID, "5", "ancient rune", "chance", pageable)).thenReturn(page);

        ResponseEntity<Page<BlockSummaryResponse>> response =
                controller.getBlocks(ROOM_ID, "5", "ancient rune", "chance", pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(page, response.getBody());
    }

    // ================= access =================

    @Nested
    @DisplayName("block access")
    class Access {

        @Test
        @DisplayName("GET /{roomId}/{blockId}/access returns the access metadata (room id is ignored)")
        void getBlockAccess() {
            BlockAccessResponse<?> body = mock(BlockAccessResponse.class);
            doReturn(body).when(blockService).getBlockAccess(BLOCK_ID);

            ResponseEntity<BlockAccessResponse<?>> response = controller.getBlockAccess(ROOM_ID, BLOCK_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(body, response.getBody());
            verify(blockService).getBlockAccess(BLOCK_ID);
        }

        @Test
        @DisplayName("POST /access/{recordId}/reset delegates the reset and returns 200")
        void resetAccess() {
            AccessRecordResponse body = mock(AccessRecordResponse.class);
            when(blockService.resetAccess(RECORD_ID)).thenReturn(body);

            ResponseEntity<AccessRecordResponse> response = controller.resetAccess(RECORD_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(body, response.getBody());
        }

        @Test
        @DisplayName("POST /access/{recordId}/grant delegates the grant and returns 200")
        void grantAccess() {
            AccessRecordResponse body = mock(AccessRecordResponse.class);
            when(blockService.grantAccess(RECORD_ID)).thenReturn(body);

            ResponseEntity<AccessRecordResponse> response = controller.grantAccess(RECORD_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(body, response.getBody());
        }

        @Test
        @DisplayName("POST /access/{recordId}/deny delegates the denial and returns 200")
        void denyAccess() {
            AccessRecordResponse body = mock(AccessRecordResponse.class);
            when(blockService.denyAccess(RECORD_ID)).thenReturn(body);

            ResponseEntity<AccessRecordResponse> response = controller.denyAccess(RECORD_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(body, response.getBody());
        }
    }

    // ================= single block reads =================

    @Nested
    @DisplayName("single block reads")
    class Reads {

        @Test
        @DisplayName("GET /{roomId}/{blockId} returns the block detail")
        void getBlock() {
            BlockDetailResponse body = mock(BlockDetailResponse.class);
            when(blockService.getBlock(BLOCK_ID)).thenReturn(body);

            ResponseEntity<BlockDetailResponse> response = controller.getBlock(ROOM_ID, BLOCK_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(body, response.getBody());
        }

        @Test
        @DisplayName("GET /{roomId}/{blockId}/wrapped returns the context-wrapped block")
        void getBlockWrapped() {
            WrappedBlockResponse body = mock(WrappedBlockResponse.class);
            when(blockService.getBlockWrapped(BLOCK_ID)).thenReturn(body);

            ResponseEntity<WrappedBlockResponse> response = controller.getBlockWrapped(ROOM_ID, BLOCK_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(body, response.getBody());
        }
    }

    // ================= roll =================

    @Test
    @DisplayName("POST /{roomId}/{blockId}/roll rolls for the current account")
    void roll() {
        Account account = account();
        RollResultResponse body = mock(RollResultResponse.class);
        when(blockService.roll(BLOCK_ID, account)).thenReturn(body);

        ResponseEntity<RollResultResponse> response = controller.roll(BLOCK_ID, account, ROOM_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(body, response.getBody());
        verify(blockService).roll(BLOCK_ID, account);
    }

    // ================= chance config =================

    @Test
    @DisplayName("PATCH /{blockId}/chance forwards the new skill/threshold pair")
    void updateChanceConfig() {
        ChanceConfigRequest request = ChanceConfigRequest.builder().skill("perception").threshold(15).build();
        ChanceConfigResponse body = mock(ChanceConfigResponse.class);
        when(blockService.updateChanceConfig(BLOCK_ID, request)).thenReturn(body);

        ResponseEntity<ChanceConfigResponse> response = controller.updateChanceConfig(BLOCK_ID, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(body, response.getBody());
        verify(blockService).updateChanceConfig(BLOCK_ID, request);
    }
}

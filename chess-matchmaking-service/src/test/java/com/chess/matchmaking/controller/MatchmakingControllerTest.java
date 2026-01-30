package com.chess.matchmaking.controller;

import com.chess.common.security.SecurityUser;
import com.chess.matchmaking.dto.JoinMatchmakingRequest;
import com.chess.matchmaking.dto.JoinMatchmakingResponse;
import com.chess.matchmaking.dto.LeaveMatchmakingRequest;
import com.chess.matchmaking.dto.MatchmakingStatusResponse;
import com.chess.matchmaking.service.MatchmakingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchmakingControllerTest {

    @Mock
    private MatchmakingService matchmakingService;

    @Test
    void join_returnsAccepted_andDelegatesToService() {
        MatchmakingController controller = new MatchmakingController(matchmakingService);

        UUID userId = UUID.randomUUID();
        SecurityUser user = new SecurityUser(userId, List.of("USER"));
        JoinMatchmakingRequest req = new JoinMatchmakingRequest();
        req.setBaseSeconds(180);
        req.setIncrementSeconds(2);
        req.setRated(true);

        when(matchmakingService.join(eq(userId), eq(180), eq(2), eq(true), eq("idem"), eq("rid")))
                .thenReturn("req-1");

        ResponseEntity<JoinMatchmakingResponse> response = controller.join("idem", "rid", req, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRequestId()).isEqualTo("req-1");
        assertThat(response.getBody().getStatus()).isEqualTo("QUEUED");

        verify(matchmakingService).join(eq(userId), eq(180), eq(2), eq(true), eq("idem"), eq("rid"));
    }

    @Test
    void leave_returnsNoContent_andDelegatesToService() {
        MatchmakingController controller = new MatchmakingController(matchmakingService);

        UUID userId = UUID.randomUUID();
        SecurityUser user = new SecurityUser(userId, List.of("USER"));
        LeaveMatchmakingRequest req = new LeaveMatchmakingRequest();
        req.setRequestId("req-1");

        ResponseEntity<Void> response = controller.leave("idem", "rid", req, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(matchmakingService).leave(eq(userId), eq("req-1"), eq("idem"), eq("rid"));
    }

    @Test
    void status_returnsOk_andDelegatesToService() {
        MatchmakingController controller = new MatchmakingController(matchmakingService);

        UUID userId = UUID.randomUUID();
        SecurityUser user = new SecurityUser(userId, List.of("USER"));

        when(matchmakingService.status(eq(userId), eq("req-1")))
                .thenReturn(new MatchmakingStatusResponse("req-1", "QUEUED", null));

        ResponseEntity<MatchmakingStatusResponse> response = controller.status("req-1", user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRequestId()).isEqualTo("req-1");
        assertThat(response.getBody().getStatus()).isEqualTo("QUEUED");
        assertThat(response.getBody().getGameId()).isNull();
    }
}


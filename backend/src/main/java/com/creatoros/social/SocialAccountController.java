package com.creatoros.social;

import com.creatoros.security.AuthenticatedUser;
import com.creatoros.social.dto.ConnectSocialAccountRequest;
import com.creatoros.social.dto.SocialAccountResponse;
import com.creatoros.sync.AccountSyncWorker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social-accounts")
@Tag(name = "Social Accounts")
public class SocialAccountController {

    private final SocialAccountService socialAccountService;
    private final AccountSyncWorker syncWorker;
    private final TaskExecutor syncTaskExecutor;

    public SocialAccountController(SocialAccountService socialAccountService,
                                    AccountSyncWorker syncWorker,
                                    @Qualifier("syncTaskExecutor") TaskExecutor syncTaskExecutor) {
        this.socialAccountService = socialAccountService;
        this.syncWorker = syncWorker;
        this.syncTaskExecutor = syncTaskExecutor;
    }

    @GetMapping
    @Operation(summary = "List the current user's connected social accounts")
    public List<SocialAccountResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return socialAccountService.listForUser(principal.userId()).stream()
                .map(SocialAccountResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Connect a new social account (YouTube, Instagram, or MOCK demo)")
    public ResponseEntity<SocialAccountResponse> connect(@AuthenticationPrincipal AuthenticatedUser principal,
                                                           @Valid @RequestBody ConnectSocialAccountRequest request) {
        SocialAccount account = socialAccountService.connect(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SocialAccountResponse.from(account));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disconnect a social account and remove its content")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        socialAccountService.disconnect(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Trigger an immediate sync for one account (async; returns 202 Accepted)")
    public ResponseEntity<Map<String, String>> triggerSync(@AuthenticationPrincipal AuthenticatedUser principal,
                                                             @PathVariable UUID id) {
        SocialAccount account = socialAccountService.requireOwned(principal.userId(), id);
        syncTaskExecutor.execute(() -> syncWorker.syncAccount(account.getId()));
        return ResponseEntity.accepted().body(Map.of(
                "message", "Sync started",
                "socialAccountId", account.getId().toString()));
    }
}

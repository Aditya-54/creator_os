package com.creatoros.content;

import com.creatoros.content.dto.ContentResponse;
import com.creatoros.content.dto.ContentSnapshotResponse;
import com.creatoros.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content")
@Tag(name = "Content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    @Operation(summary = "List the current user's content across all connected platforms")
    public Page<ContentResponse> list(@AuthenticationPrincipal AuthenticatedUser principal, Pageable pageable) {
        return contentService.list(principal.userId(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single piece of content")
    public ContentResponse get(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return contentService.get(principal.userId(), id);
    }

    @GetMapping("/{id}/snapshots")
    @Operation(summary = "Get the full historical performance timeline for one content item")
    public List<ContentSnapshotResponse> snapshots(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return contentService.snapshots(principal.userId(), id);
    }
}

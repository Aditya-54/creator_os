package com.creatoros.demo;

import com.creatoros.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets a logged-in user populate their own workspace with a clearly-labeled
 * demo dataset (two demo accounts, ~14 content items, rich historical
 * snapshots) so the product can be evaluated with zero social API
 * credentials. See docs/demo-script.md.
 */
@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo Data")
public class DemoController {

    private final DemoDataGenerator demoDataGenerator;

    public DemoController(DemoDataGenerator demoDataGenerator) {
        this.demoDataGenerator = demoDataGenerator;
    }

    @PostMapping("/seed")
    @Operation(summary = "Populate the current user's workspace with demo content, snapshots, topics, and brand mentions")
    public ResponseEntity<Map<String, String>> seed(@AuthenticationPrincipal AuthenticatedUser principal) {
        demoDataGenerator.seedForUser(principal.userId());
        return ResponseEntity.ok(Map.of("message", "Demo workspace seeded"));
    }

    @DeleteMapping("/reset")
    @Operation(summary = "Remove all demo accounts/content for the current user")
    public ResponseEntity<Void> reset(@AuthenticationPrincipal AuthenticatedUser principal) {
        demoDataGenerator.resetForUser(principal.userId());
        return ResponseEntity.noContent().build();
    }
}

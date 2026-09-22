package com.creatoros.ai;

import com.creatoros.ai.dto.AiQueryRequest;
import com.creatoros.ai.dto.AiQueryResponse;
import com.creatoros.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Analyst")
public class AiController {

    private final AiAnalystService aiAnalystService;

    public AiController(AiAnalystService aiAnalystService) {
        this.aiAnalystService = aiAnalystService;
    }

    @PostMapping("/query")
    @Operation(summary = "Ask a natural-language question about your content; grounded in real analytics via tool calls")
    public AiQueryResponse query(@AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody AiQueryRequest request) {
        return aiAnalystService.query(principal.userId(), request);
    }
}

package com.creatoros.ai;

import com.creatoros.ai.tools.AnalyticsTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes {@link AnalyticsTools} over the standalone MCP server
 * (spring-ai-starter-mcp-server-webmvc autoconfiguration picks up any
 * {@link ToolCallbackProvider} bean automatically). Point an MCP-compatible
 * client (Claude Desktop, etc.) at this application's {@code /mcp} endpoint
 * to use these tools directly - see docs/ai-and-mcp.md.
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider analyticsToolCallbackProvider(AnalyticsTools analyticsTools) {
        return MethodToolCallbackProvider.builder().toolObjects(analyticsTools).build();
    }
}

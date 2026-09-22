package com.creatoros.social.integration.youtube;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "creatoros.social.youtube")
public record YouTubeProperties(String apiKey, String clientId, String clientSecret) {

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}

package com.creatoros.social.integration;

import com.creatoros.social.Platform;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Resolves the correct {@link SocialPlatformClient} adapter for a given platform. */
@Component
public class SocialPlatformClientRegistry {

    private final Map<Platform, SocialPlatformClient> clientsByPlatform;

    public SocialPlatformClientRegistry(List<SocialPlatformClient> clients) {
        this.clientsByPlatform = clients.stream()
                .collect(Collectors.toMap(SocialPlatformClient::platform, Function.identity()));
    }

    public SocialPlatformClient resolve(Platform platform) {
        SocialPlatformClient client = clientsByPlatform.get(platform);
        if (client == null) {
            throw new IllegalStateException("No SocialPlatformClient registered for platform " + platform);
        }
        return client;
    }
}

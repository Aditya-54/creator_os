package com.creatoros.social.dto;

import com.creatoros.social.Platform;
import jakarta.validation.constraints.NotNull;

public record ConnectSocialAccountRequest(
        @NotNull Platform platform,
        String handle
) {
}

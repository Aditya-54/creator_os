package com.creatoros.security;

import java.util.UUID;

/** Principal placed into the {@link org.springframework.security.core.context.SecurityContext} for each request. */
public record AuthenticatedUser(UUID userId, String email) {
}

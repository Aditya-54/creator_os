package com.creatoros.social.integration;

import java.util.UUID;

/**
 * Persistence-free view of a {@code SocialAccount} passed to platform adapters.
 * Keeping adapters independent of the JPA entity means sync workers can call
 * them after a short claiming transaction has already committed (and the
 * entity has gone detached) without risking lazy-initialization errors.
 */
public record ConnectedAccount(UUID id, String platformAccountId, String username) {
}

package com.creatoros.sync;

import com.creatoros.social.Platform;
import java.util.UUID;

/**
 * Everything a worker needs to run one sync, captured as plain values (not JPA
 * entities) at the moment the account lock is claimed. Crossing the boundary
 * to an external API call with detached entities risks
 * {@code LazyInitializationException}; this avoids that entirely.
 */
public record SyncContext(UUID syncJobId, UUID accountId, Platform platform, String platformAccountId, String username) {
}

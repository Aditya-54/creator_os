package com.creatoros.sync;

import java.util.UUID;

/** Thrown by {@link AccountLockService#claim} when another worker already holds this account's sync. */
public class SyncAlreadyInProgressException extends RuntimeException {

    public SyncAlreadyInProgressException(UUID accountId) {
        super("A sync is already in progress for social account " + accountId);
    }
}

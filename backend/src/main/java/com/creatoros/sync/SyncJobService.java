package com.creatoros.sync;

import com.creatoros.social.SocialAccount;
import com.creatoros.social.SocialAccountRepository;
import com.creatoros.social.SocialAccountStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncJobService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final SyncJobRepository syncJobRepository;
    private final SocialAccountRepository socialAccountRepository;

    public SyncJobService(SyncJobRepository syncJobRepository, SocialAccountRepository socialAccountRepository) {
        this.syncJobRepository = syncJobRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    @Transactional
    public void markRetrying(UUID jobId, int attempt, String errorMessage) {
        SyncJob job = syncJobRepository.getReferenceById(jobId);
        job.setStatus(SyncStatus.RETRYING);
        job.setAttemptCount(attempt);
        job.setErrorMessage(truncate(errorMessage));
    }

    @Transactional
    public void markFailed(UUID jobId, UUID accountId, String errorMessage) {
        SyncJob job = syncJobRepository.getReferenceById(jobId);
        job.setStatus(SyncStatus.FAILED);
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(truncate(errorMessage));

        SocialAccount account = socialAccountRepository.getReferenceById(accountId);
        account.setStatus(SocialAccountStatus.ERROR);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > MAX_ERROR_MESSAGE_LENGTH ? message.substring(0, MAX_ERROR_MESSAGE_LENGTH) : message;
    }
}

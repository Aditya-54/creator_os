package com.creatoros.social;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    List<SocialAccount> findByUserId(UUID userId);

    Optional<SocialAccount> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByPlatformAndPlatformAccountId(Platform platform, String platformAccountId);

    /**
     * Row-level lock acquired for the duration of a sync so two workers can never
     * process the same account concurrently. See docs/concurrency.md for why
     * pessimistic locking was chosen over optimistic retries for this path.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SocialAccount a where a.id = :id")
    Optional<SocialAccount> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select a from SocialAccount a
            where a.status = com.creatoros.social.SocialAccountStatus.CONNECTED
              and (a.lastSyncedAt is null or a.lastSyncedAt < :staleBefore)
            """)
    List<SocialAccount> findDueForSync(@Param("staleBefore") Instant staleBefore);
}

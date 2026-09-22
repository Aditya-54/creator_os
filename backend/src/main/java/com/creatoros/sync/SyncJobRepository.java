package com.creatoros.sync;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {

    boolean existsBySocialAccount_IdAndStatusIn(UUID socialAccountId, Collection<SyncStatus> statuses);

    @Query("select j from SyncJob j where j.socialAccount.id = :accountId order by j.createdAt desc")
    List<SyncJob> findRecentForAccount(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("""
            select j from SyncJob j
            where j.socialAccount.user.id = :userId
            order by j.createdAt desc
            """)
    List<SyncJob> findRecentForUser(@Param("userId") UUID userId, Pageable pageable);
}

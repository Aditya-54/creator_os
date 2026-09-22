package com.creatoros.analytics;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViralEventRepository extends JpaRepository<ViralEvent, UUID> {

    List<ViralEvent> findByContentIdOrderByStartTimeDesc(UUID contentId);

    Optional<ViralEvent> findByContentIdAndStartTime(UUID contentId, Instant startTime);

    @Query("""
            select v from ViralEvent v
            where v.content.socialAccount.user.id = :userId
            order by v.detectedAt desc
            """)
    List<ViralEvent> findRecentForUser(@Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);
}

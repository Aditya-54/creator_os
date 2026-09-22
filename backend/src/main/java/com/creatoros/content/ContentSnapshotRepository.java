package com.creatoros.content;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentSnapshotRepository extends JpaRepository<ContentSnapshot, UUID> {

    List<ContentSnapshot> findByContentIdOrderByCapturedAtAsc(UUID contentId);

    Optional<ContentSnapshot> findFirstByContentIdOrderByCapturedAtDesc(UUID contentId);

    boolean existsByContentIdAndCapturedAt(UUID contentId, Instant capturedAt);

    @Query("""
            select s from ContentSnapshot s
            where s.content.id = :contentId and s.capturedAt between :from and :to
            order by s.capturedAt asc
            """)
    List<ContentSnapshot> findWindow(@Param("contentId") UUID contentId,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);
}

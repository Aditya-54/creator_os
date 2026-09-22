package com.creatoros.content;

import com.creatoros.social.Platform;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, UUID> {

    Optional<Content> findByPlatformAndPlatformContentId(Platform platform, String platformContentId);

    @Query("""
            select c from Content c
            where c.socialAccount.user.id = :userId
            order by c.publishedAt desc nulls last
            """)
    Page<Content> findAllForUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select c from Content c
            where c.socialAccount.user.id = :userId and c.id = :contentId
            """)
    Optional<Content> findForUser(@Param("userId") UUID userId, @Param("contentId") UUID contentId);

    List<Content> findBySocialAccountId(UUID socialAccountId);

    @Query("""
            select c from Content c
            where c.socialAccount.user.id = :userId
            order by c.publishedAt desc nulls last
            """)
    List<Content> findRecentForUser(@Param("userId") UUID userId, Pageable pageable);

    long countBySocialAccount_User_Id(UUID userId);

    /**
     * Each content item paired with its most recent snapshot ("current" totals).
     * Computed in-memory-friendly form here rather than a heavier SQL rollup,
     * since a single creator's dataset is small (tens-hundreds of items).
     */
    @Query("""
            select c, s from Content c
            join ContentSnapshot s on s.content = c
            where c.socialAccount.user.id = :userId
              and s.capturedAt = (select max(s2.capturedAt) from ContentSnapshot s2 where s2.content = c)
            """)
    List<Object[]> findLatestContentWithSnapshotForUser(@Param("userId") UUID userId);
}

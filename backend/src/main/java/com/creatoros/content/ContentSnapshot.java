package com.creatoros.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A single point-in-time performance observation. Append-only by design: the
 * sync worker always INSERTs a new row, never UPDATEs an existing one, so the
 * full historical growth curve is preserved (see docs/database.md).
 */
@Entity
@Table(name = "content_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(nullable = false)
    @Builder.Default
    private long views = 0;

    @Column(nullable = false)
    @Builder.Default
    private long likes = 0;

    @Column(nullable = false)
    @Builder.Default
    private long comments = 0;

    @Column(nullable = false)
    @Builder.Default
    private long shares = 0;

    @Column(name = "followers_attributed")
    private Long followersAttributed;

    @Column(name = "watch_time_seconds")
    private Long watchTimeSeconds;

    @Column(name = "avg_view_duration_seconds")
    private Double avgViewDurationSeconds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

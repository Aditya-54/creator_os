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

/** Latest known regional breakdown for a content item; refreshed (not appended) on each sync. */
@Entity
@Table(name = "content_region_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentRegionMetric {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(nullable = false, length = 8)
    private String country;

    @Column(nullable = false)
    @Builder.Default
    private long views = 0;

    @Column(nullable = false)
    @Builder.Default
    private long engagement = 0;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}

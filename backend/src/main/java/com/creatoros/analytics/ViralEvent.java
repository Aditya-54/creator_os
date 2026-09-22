package com.creatoros.analytics;

import com.creatoros.content.Content;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "viral_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViralEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "peak_growth", nullable = false)
    private double peakGrowth;

    @Column(name = "baseline_growth", nullable = false)
    private double baselineGrowth;

    @Column(nullable = false)
    private double multiplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ViralConfidence confidence;

    @Column(columnDefinition = "TEXT")
    private String explanation;
}

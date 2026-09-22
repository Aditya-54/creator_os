package com.creatoros.analytics;

import com.creatoros.content.Content;
import com.creatoros.content.ContentRepository;
import com.creatoros.content.ContentSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shared read helper: "each content item plus its most recent snapshot" for a user. */
@Service
public class AnalyticsQueryService {

    private final ContentRepository contentRepository;

    public AnalyticsQueryService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Transactional(readOnly = true)
    public List<ContentWithSnapshot> loadLatestForUser(UUID userId) {
        return contentRepository.findLatestContentWithSnapshotForUser(userId).stream()
                .map(row -> new ContentWithSnapshot((Content) row[0], (ContentSnapshot) row[1]))
                .toList();
    }
}

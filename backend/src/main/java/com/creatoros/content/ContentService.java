package com.creatoros.content;

import com.creatoros.content.dto.ContentResponse;
import com.creatoros.content.dto.ContentSnapshotResponse;
import com.creatoros.exception.ResourceNotFoundException;
import com.creatoros.topic.ContentTopicRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentSnapshotRepository snapshotRepository;
    private final ContentTopicRepository contentTopicRepository;

    public ContentService(ContentRepository contentRepository,
                           ContentSnapshotRepository snapshotRepository,
                           ContentTopicRepository contentTopicRepository) {
        this.contentRepository = contentRepository;
        this.snapshotRepository = snapshotRepository;
        this.contentTopicRepository = contentTopicRepository;
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> list(UUID userId, Pageable pageable) {
        return contentRepository.findAllForUser(userId, pageable)
                .map(c -> ContentResponse.from(c, topicNames(c.getId())));
    }

    @Transactional(readOnly = true)
    public ContentResponse get(UUID userId, UUID contentId) {
        Content content = requireOwned(userId, contentId);
        return ContentResponse.from(content, topicNames(contentId));
    }

    @Transactional(readOnly = true)
    public List<ContentSnapshotResponse> snapshots(UUID userId, UUID contentId) {
        requireOwned(userId, contentId);
        return snapshotRepository.findByContentIdOrderByCapturedAtAsc(contentId).stream()
                .map(ContentSnapshotResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Content requireOwned(UUID userId, UUID contentId) {
        return contentRepository.findForUser(userId, contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
    }

    private List<String> topicNames(UUID contentId) {
        return contentTopicRepository.findByContentId(contentId).stream()
                .map(ct -> ct.getTopic().getName())
                .toList();
    }
}

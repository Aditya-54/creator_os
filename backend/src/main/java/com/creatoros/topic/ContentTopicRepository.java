package com.creatoros.topic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentTopicRepository extends JpaRepository<ContentTopic, ContentTopicId> {

    @Query("select ct from ContentTopic ct where ct.content.id = :contentId and ct.topic.id = :topicId")
    Optional<ContentTopic> findByContentIdAndTopicId(@Param("contentId") UUID contentId, @Param("topicId") UUID topicId);

    @Query("select ct from ContentTopic ct where ct.content.id = :contentId")
    List<ContentTopic> findByContentId(@Param("contentId") UUID contentId);

    @Query("select ct from ContentTopic ct where ct.content.socialAccount.user.id = :userId")
    List<ContentTopic> findByUserId(@Param("userId") UUID userId);
}

package com.creatoros.brand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentBrandMentionRepository extends JpaRepository<ContentBrandMention, UUID> {

    List<ContentBrandMention> findByContentId(UUID contentId);

    Optional<ContentBrandMention> findByContentIdAndBrandId(UUID contentId, UUID brandId);

    List<ContentBrandMention> findByContent_SocialAccount_User_Id(UUID userId);
}

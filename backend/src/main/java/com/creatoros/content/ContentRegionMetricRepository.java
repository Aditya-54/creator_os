package com.creatoros.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRegionMetricRepository extends JpaRepository<ContentRegionMetric, UUID> {

    List<ContentRegionMetric> findByContentId(UUID contentId);

    Optional<ContentRegionMetric> findByContentIdAndCountry(UUID contentId, String country);

    List<ContentRegionMetric> findByContent_SocialAccount_User_Id(UUID userId);
}

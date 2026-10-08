package com.cozyfireplace.server.images;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, String> {

    Page<ImageAsset> findByProfileIdOrderByCreatedAtDesc(Long profileId, Pageable pageable);

    Optional<ImageAsset> findByIdAndProfileId(String id, Long profileId);

    @Query("SELECT COALESCE(SUM(i.sizeBytes), 0) FROM ImageAsset i WHERE i.profileId = :profileId")
    long sumSizeBytesByProfileId(@Param("profileId") Long profileId);
}

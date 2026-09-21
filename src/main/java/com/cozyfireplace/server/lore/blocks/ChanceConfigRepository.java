package com.cozyfireplace.server.lore.blocks;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChanceConfigRepository extends JpaRepository<ChanceConfig, Long> {
    Optional<ChanceConfig> findByBlockId(Long blockId);
}
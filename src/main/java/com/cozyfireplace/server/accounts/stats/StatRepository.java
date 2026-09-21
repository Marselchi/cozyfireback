package com.cozyfireplace.server.accounts.stats;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StatRepository extends JpaRepository<Stat, String> {
}
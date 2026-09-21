package com.cozyfireplace.server.accounts.stats;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, String> {

    List<Skill> findAllBySystem(GameSystem gameSystem);
}
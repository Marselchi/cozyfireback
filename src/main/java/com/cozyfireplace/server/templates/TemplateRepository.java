package com.cozyfireplace.server.templates;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    Page<Template> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    Optional<Template> findByName(String name);
}

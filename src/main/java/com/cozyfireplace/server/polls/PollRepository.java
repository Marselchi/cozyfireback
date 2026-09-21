package com.cozyfireplace.server.polls;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> getPollsBySource(String source);
}

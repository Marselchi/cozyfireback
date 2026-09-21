package com.cozyfireplace.server.polls;


import com.cozyfireplace.server.auth.UserDetailsImpl;
import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.polls.dto.PollCreateRequest;
import com.cozyfireplace.server.polls.dto.PollMapper;
import com.cozyfireplace.server.polls.dto.PollResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PollService {
    private final PollRepository pollRepository;
    private final PollMapper pollMapper;

    public void createPoll(PollCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) return;

        Profile currentUser = ((UserDetailsImpl) Objects.requireNonNull(authentication.getPrincipal())).getProfile();
        Poll poll = Poll.builder()
                .source(request.source())
                .content(request.content())
                .profile(currentUser)
                .build();
        pollRepository.save(poll);
    }

    public List<PollResponse> getPolls(String source) {
        List<Poll> polls = pollRepository.getPollsBySource(source);

        return polls.stream().map(pollMapper::toPollResponse).toList();
    }
}

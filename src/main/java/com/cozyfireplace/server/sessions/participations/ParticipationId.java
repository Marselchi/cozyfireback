package com.cozyfireplace.server.sessions.participations;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationId implements Serializable {

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "account_id")
    private Long accountId;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParticipationId that)) return false;
        return Objects.equals(sessionId, that.sessionId) && Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, accountId);
    }
}
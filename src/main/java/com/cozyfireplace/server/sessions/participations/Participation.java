package com.cozyfireplace.server.sessions.participations;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;


@Entity
@Table(name = "participations")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participation {
    @EmbeddedId
    private ParticipationId id;

    private Boolean accepted;
}

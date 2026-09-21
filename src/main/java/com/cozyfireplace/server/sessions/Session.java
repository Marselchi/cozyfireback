package com.cozyfireplace.server.sessions;


import com.cozyfireplace.server.accounts.Account;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "creator_account_id", nullable = false)
    private Account creator;

    private Instant time;
    @Column(columnDefinition = "TEXT")
    private String description;
}

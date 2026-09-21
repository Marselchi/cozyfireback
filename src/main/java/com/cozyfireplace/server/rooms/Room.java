package com.cozyfireplace.server.rooms;

import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.accounts.stats.GameSystem;
import com.github.slugify.Slugify;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "creator_account_id", nullable = false)
    private Account creator;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String url;

    private GameSystem gameSystem;

    @PrePersist
    @PreUpdate
    private void ensureUrl() {
        if (this.url == null || this.url.isBlank()) {
            this.url = generateUrl(this.name);
        }
    }

    private String generateUrl(String url) {
        return Slugify.builder().transliterator(true).build().slugify(url)
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

}
package com.cozyfireplace.server.accounts.stats;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stats")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stat {
    @Id
    @Column(name = "key")
    private String key;

    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "system")
    private GameSystem system;

    @Column(name = "sort_order")
    private Integer sortOrder;
}

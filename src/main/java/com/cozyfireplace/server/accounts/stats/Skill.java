package com.cozyfireplace.server.accounts.stats;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skills")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    @Id
    @Column(name = "key")
    private String key;

    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "system")
    private GameSystem system;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stat_key")
    private Stat stat;

    @Column(name = "sort_order")
    private Integer sortOrder;
}

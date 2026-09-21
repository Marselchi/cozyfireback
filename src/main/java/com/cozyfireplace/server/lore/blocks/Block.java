package com.cozyfireplace.server.lore.blocks;

import com.cozyfireplace.server.lore.Lore;
import com.cozyfireplace.server.roles.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "block")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lore_id")
    private Lore lore;

    @OneToOne(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    private ChanceConfig chanceConfig;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "block_roles",
            joinColumns = @JoinColumn(name = "block_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public boolean hasChance() {
        return chanceConfig != null;
    }
}
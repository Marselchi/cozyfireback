package com.cozyfireplace.server.lore.blocks;

import com.cozyfireplace.server.accounts.stats.Skill;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chance_config")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChanceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int threshold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_key")
    private Skill skill;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;
}
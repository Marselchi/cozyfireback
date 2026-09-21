package com.cozyfireplace.server.accounts;

import com.cozyfireplace.server.accounts.stats.AccountCharSkill;
import com.cozyfireplace.server.accounts.stats.AccountCharStat;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "account_chars")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountChar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Version
    private Integer version;

    private String race;

    private String origin;

    @Column(name = "character_class")
    private String characterClass;

    @Column(name = "level")
    @Builder.Default
    private Integer level = 1;

    @OneToMany(mappedBy = "accountChar", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AccountCharStat> stats = new HashSet<>();

    @OneToMany(mappedBy = "accountChar", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AccountCharSkill> skills = new HashSet<>();
}
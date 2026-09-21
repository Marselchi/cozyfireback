package com.cozyfireplace.server.accounts.stats;

import com.cozyfireplace.server.accounts.AccountChar;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_char_skills", uniqueConstraints = @UniqueConstraint(columnNames = {"account_char_id", "skill_key"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountCharSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_char_id")
    private AccountChar accountChar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_key")
    private Skill skill;
    
    private Integer proficiency;

    private Integer modifier;

}
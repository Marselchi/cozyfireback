package com.cozyfireplace.server.accounts.stats;

import com.cozyfireplace.server.accounts.AccountChar;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_char_stats", uniqueConstraints = @UniqueConstraint(columnNames = {"account_char_id", "stat_key"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountCharStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_char_id")
    private AccountChar accountChar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stat_key")
    private Stat stat;

    private Integer score;

    private Integer modifier;

}
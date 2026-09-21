package com.cozyfireplace.server.lore.blocks;

import com.cozyfireplace.server.accounts.AccountChar;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "access_record", uniqueConstraints = @UniqueConstraint(columnNames = {"block_id", "account_char_id"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_char_id")
    private AccountChar accountChar;

    @Enumerated(EnumType.STRING)
    private AccessStatus status;

    private Integer rollValue;

    private Integer modifier;

    public boolean isPassed() {
        return status == AccessStatus.PASSED || status == AccessStatus.GRANTED;
    }

    public boolean isRestricted() {
        return status == AccessStatus.FAIL || status == AccessStatus.DENIED;
    }
}
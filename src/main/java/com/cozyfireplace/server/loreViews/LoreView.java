package com.cozyfireplace.server.loreViews;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.lore.Lore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lore_view")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoreView {

    @EmbeddedId
    private LoreViewId id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "lore_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Lore lore;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Account account;
    /*
    * True - viewed
    * False - viewed but lore was updated
    */
    @Column(nullable = false)
    private boolean viewed;
}

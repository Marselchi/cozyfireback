package com.cozyfireplace.server.loreViews;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class LoreViewId implements Serializable {

    @Column(name = "lore_id")
    private Long loreId;

    @Column(name = "account_id")
    private Long accountId;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LoreViewId that)) return false;
        return Objects.equals(loreId, that.loreId) && Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loreId, accountId);
    }
}
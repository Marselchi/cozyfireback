package com.cozyfireplace.server.lore;


import com.cozyfireplace.server.accounts.Account;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.tags.Tag;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.util.Set;

@Entity
@Table(name = "lore")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Length(message = "Max length of title is 100", max = 100)
    @NotNull(message = "Title cannot be null")
    @NotEmpty(message = "Title cannot be empty")
    @Column(columnDefinition = "TEXT")
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String date;
    @NotEmpty(message = "Content cannot be empty")
    @NotNull(message = "Content cannot be null")
    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lore_roles",
            joinColumns = @JoinColumn(name = "lore_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lore_tags",
            joinColumns = @JoinColumn(name = "lore_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;
}

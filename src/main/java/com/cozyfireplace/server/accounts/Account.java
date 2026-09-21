package com.cozyfireplace.server.accounts;

import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.roles.Role;
import com.cozyfireplace.server.rooms.Room;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "accounts")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "account_roles",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    // Если планируется несколько персонажей на один аккаунт, замените @OneToOne на @ManyToOne
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "account_char_id")
    private AccountChar accountChar;

}

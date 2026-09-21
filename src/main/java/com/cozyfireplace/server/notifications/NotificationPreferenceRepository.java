package com.cozyfireplace.server.notifications;

import com.cozyfireplace.server.notifications.dto.AccountChannelView;
import com.cozyfireplace.server.notifications.dto.NotificationEventCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    @Query("""
        select np.account.id as accountId, np.channel as channel
        from NotificationPreference np
        join np.account acc
        where acc.room.id = :roomId
          and acc.id <> :authorId
          and np.eventCode = :eventCode
          and np.enabled = true
          and (:restrictByRoles = false or exists (
                select r from acc.roles r where r.id in :roleIds
              ))
    """)
    List<AccountChannelView> findAccountsWithChannelsRestricted(
            @Param("roomId") Long roomId,
            @Param("eventCode") NotificationEventCode eventCode,
            @Param("authorId") Long authorId,
            @Param("restrictByRoles") boolean restrictByRoles,
            @Param("roleIds") Set<Long> roleIds
    );

    @Query("""
        select
            np.account.id as accountId,
            np.channel as channel
        from NotificationPreference np
        join np.account acc
        where acc.room.id = :roomId
          and acc.id <> :authorId
          and np.eventCode = :eventCode
          and np.enabled = true
    """)
    List<AccountChannelView> findAccountsWithChannels(
            @Param("roomId") Long roomId,
            @Param("eventCode") NotificationEventCode eventCode,
            @Param("authorId") Long authorId
    );

    @Query("""
        select np.account.id as accountId, np.channel as channel
        from NotificationPreference np
        where np.account.id = :recipientId
          and np.eventCode = :eventCode
          and np.enabled = true
    """)
    List<AccountChannelView> findChannelsForAccount(
                                                      @Param("recipientId") Long recipientId,
                                                      @Param("eventCode") NotificationEventCode eventCode
    );

    @Query("""
    select np.account.id as accountId, np.channel as channel
    from NotificationPreference np
    join np.account acc
    where acc.room.id = :roomId
      and acc.id <> :authorId
      and np.eventCode = :eventCode
      and np.enabled = true
      and (:restrictByRoles = false or exists (
            select r from acc.roles r where r.id in :roleIds
          ))
      and exists (
            select lv from LoreView lv
            where lv.account.id = acc.id and lv.lore.id = :loreId
          )
""")
    List<AccountChannelView> findAccountsWithChannelsRestrictedLoreUpdated(
            @Param("roomId") Long roomId,
            @Param("eventCode") NotificationEventCode eventCode,
            @Param("authorId") Long authorId,
            @Param("restrictByRoles") boolean restrictByRoles,
            @Param("roleIds") Set<Long> roleIds,
            @Param("loreId") Long loreId
    );

    List<NotificationPreference> findAllByAccountId(Long accountId);
}

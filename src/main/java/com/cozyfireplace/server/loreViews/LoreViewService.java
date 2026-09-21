package com.cozyfireplace.server.loreViews;


import com.cozyfireplace.server.accounts.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoreViewService {

    private final LoreViewRepository loreViewRepository;

    public void setViewed(Long accountId, Long loreId){
        loreViewRepository.upsertViewed(accountId, loreId);
    }

    public void loreUpdated(Long loreId){
        int updatedCount = loreViewRepository.resetViewedByLoreId(loreId);
    }
}

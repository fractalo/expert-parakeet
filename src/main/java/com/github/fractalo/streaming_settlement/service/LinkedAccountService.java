package com.github.fractalo.streaming_settlement.service;

import com.github.fractalo.streaming_settlement.domain.LinkedAccount;
import com.github.fractalo.streaming_settlement.repository.LinkedAccountRepository;
import com.github.fractalo.streaming_settlement.oauth2.user.OAuth2UserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkedAccountService {
    private final LinkedAccountRepository linkedAccountRepository;

    public LinkedAccount findAccount(String providerId, String providerUserId) {
        return linkedAccountRepository.findByProviderIdAndProviderUserId(providerId, providerUserId)
                .orElse(null);
    }

    @Transactional
    public LinkedAccount createOrUpdateAccount(OAuth2UserDetail oAuth2UserDetail) {
        String providerId = oAuth2UserDetail.getProviderId();
        String providerUserId = oAuth2UserDetail.getProviderUserId();

        LinkedAccount existingAccount = findAccount(providerId, providerUserId);

        if (existingAccount != null) {
            existingAccount.update(oAuth2UserDetail);
            return existingAccount;
        } else {
            LinkedAccount linkedAccount = new LinkedAccount(oAuth2UserDetail);
            return linkedAccountRepository.save(linkedAccount);
        }
    }
}

package com.github.fractalo.streaming_settlement.security.user;

import com.github.fractalo.streaming_settlement.security.OAuth2Provider;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuth2UserDetailFactory implements OAuth2UserDetailFactory {
    private final OAuth2Provider provider = OAuth2Provider.GOOGLE;

    @Override
    public boolean supports(String providerId) {
        return provider.getId().equals(providerId);
    }

    @Override
    public OAuth2UserDetail create(OAuth2User oAuth2User) {
        return new GoogleOAuth2UserDetail(provider.getId(), oAuth2User);
    }
}

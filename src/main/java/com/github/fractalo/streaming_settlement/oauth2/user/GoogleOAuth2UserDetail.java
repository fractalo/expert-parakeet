package com.github.fractalo.streaming_settlement.oauth2.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RequiredArgsConstructor
public class GoogleOAuth2UserDetail implements OAuth2UserDetail {
    private final String providerId;
    private final OAuth2User oAuth2User;

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public String getProviderUserId() {
        return oAuth2User.getAttribute("sub");
    }

    @Override
    public String getEmail() {
        return oAuth2User.getAttribute("email");
    }
}

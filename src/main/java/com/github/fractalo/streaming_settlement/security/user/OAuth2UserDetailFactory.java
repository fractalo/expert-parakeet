package com.github.fractalo.streaming_settlement.security.user;

import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2UserDetailFactory {
    boolean supports(String providerId);
    OAuth2UserDetail create(OAuth2User oAuth2User);
}

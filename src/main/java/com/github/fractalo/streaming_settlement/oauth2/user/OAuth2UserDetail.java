package com.github.fractalo.streaming_settlement.oauth2.user;

public interface OAuth2UserDetail {
    String getProviderId();
    String getProviderUserId();
    String getEmail();
}

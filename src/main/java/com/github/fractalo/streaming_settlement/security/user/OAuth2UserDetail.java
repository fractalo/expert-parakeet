package com.github.fractalo.streaming_settlement.security.user;

public interface OAuth2UserDetail {
    String getProviderId();
    String getProviderUserId();
    String getEmail();
}

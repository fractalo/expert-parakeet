package com.github.fractalo.streaming_settlement.oauth2;

import com.github.fractalo.streaming_settlement.domain.Member;
import com.github.fractalo.streaming_settlement.oauth2.user.OAuth2UserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {
    private final Member member;
    private final OAuth2UserDetail oAuth2UserDetail;

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of(
                "provider", oAuth2UserDetail.getProviderId()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return member.getId().toString();
    }
}

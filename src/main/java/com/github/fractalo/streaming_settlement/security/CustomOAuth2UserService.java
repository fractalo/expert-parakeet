package com.github.fractalo.streaming_settlement.security;

import com.github.fractalo.streaming_settlement.domain.LinkedAccount;
import com.github.fractalo.streaming_settlement.domain.Member;
import com.github.fractalo.streaming_settlement.security.user.OAuth2UserDetail;
import com.github.fractalo.streaming_settlement.security.user.OAuth2UserDetailFactory;
import com.github.fractalo.streaming_settlement.service.LinkedAccountService;
import com.github.fractalo.streaming_settlement.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final List<OAuth2UserDetailFactory> userDetailFactories;
    private final LinkedAccountService linkedAccountService;
    private final MemberService memberService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserDetail oAuth2UserDetail = createUserDetail(registrationId, oAuth2User);

        LinkedAccount linkedAccount = linkedAccountService.createOrUpdateAccount(oAuth2UserDetail);

        Member member = Optional.ofNullable(linkedAccount.getMember())
                .orElseGet(() -> memberService.createMemberFrom(linkedAccount));

        return new CustomOAuth2User(member, oAuth2UserDetail);
    }

    private OAuth2UserDetail createUserDetail(String registrationId, OAuth2User oAuth2User) {
        for (OAuth2UserDetailFactory userDetailFactory : userDetailFactories) {
            if (userDetailFactory.supports(registrationId)) {
                return userDetailFactory.create(oAuth2User);
            }
        }
        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    }
}

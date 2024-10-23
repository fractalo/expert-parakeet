package com.github.fractalo.streaming_settlement.domain;

import com.github.fractalo.streaming_settlement.oauth2.user.OAuth2UserDetail;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = {
        @Index(name = "unique_linked_account_member_id_provider_id",
                columnList = "member_id, provider_id", unique = true),
        @Index(name = "unique_linked_account_provider_user_id_provider_id",
                columnList = "provider_user_id, provider_id", unique = true)
})
public class LinkedAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "linked_account_id")
    private Long id;

    @NotNull
    private String providerId;

    @NotNull
    private String providerUserId;

    private String providerEmail;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public LinkedAccount(OAuth2UserDetail userDetail) {
        this.providerId = userDetail.getProviderId();
        this.providerUserId = userDetail.getProviderUserId();
        this.providerEmail = userDetail.getEmail();
    }

    public void update(OAuth2UserDetail userDetail) {
        this.providerEmail = userDetail.getEmail();
    }

}

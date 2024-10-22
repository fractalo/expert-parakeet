package com.github.fractalo.streaming_settlement.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Email
    @Column(unique = true)
    private String email;

    @NotNull
    private String displayName = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);

    @OneToMany(mappedBy = "member")
    private List<LinkedAccount> linkedAccounts = new ArrayList<>();

    public Member(LinkedAccount linkedAccount) {
        this.email = linkedAccount.getProviderEmail();
    }
}

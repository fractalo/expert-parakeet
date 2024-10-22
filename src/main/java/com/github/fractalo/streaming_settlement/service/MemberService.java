package com.github.fractalo.streaming_settlement.service;

import com.github.fractalo.streaming_settlement.domain.LinkedAccount;
import com.github.fractalo.streaming_settlement.domain.Member;
import com.github.fractalo.streaming_settlement.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    @Transactional
    public Member createMemberFrom(LinkedAccount linkedAccount) {
        String email = linkedAccount.getProviderEmail();
        Optional<Member> existingMember = memberRepository.findByEmail(email);

        if (existingMember.isPresent()) {
            throw new IllegalStateException("Member already exists for email: " + email);
        }

        Member member = new Member(linkedAccount);
        linkedAccount.setMember(member);

        return memberRepository.save(member);
    }
}

package com.github.fractalo.streaming_settlement.dto;

import com.github.fractalo.streaming_settlement.domain.Member;

public record SimpleMember(
        Long id,
        String displayName
) {
    public SimpleMember(Member member) {
        this(member.getId(), member.getDisplayName());
    }
}

package com.github.fractalo.streaming_settlement.repository;

import com.github.fractalo.streaming_settlement.domain.LinkedAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {
    @EntityGraph(attributePaths = "member")
    Optional<LinkedAccount> findByProviderIdAndProviderUserId(String providerId, String providerUserId);
}

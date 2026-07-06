package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByMemberId(Long memberId);

	Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}

package com.dongnemarket.auth.repository;

import com.dongnemarket.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

	Optional<EmailVerification> findByEmail(String email);

	/** 회원가입 시 해당 이메일이 인증 완료 상태인지 확인할 때 사용 (그룹 2-4에서 연동) */
	boolean existsByEmailAndVerifiedTrue(String email);
}

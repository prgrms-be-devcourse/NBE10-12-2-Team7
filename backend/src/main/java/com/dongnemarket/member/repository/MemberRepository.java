package com.dongnemarket.member.repository;

import com.dongnemarket.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);
}

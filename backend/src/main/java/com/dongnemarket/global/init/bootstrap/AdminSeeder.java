package com.dongnemarket.global.init.bootstrap;

import com.dongnemarket.global.init.DataSeeder;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부트스트랩: 관리자(ROLE_ADMIN) 계정 1개를 시드한다(멱등).
 * 같은 이메일이 이미 있으면 만들지 않는다. test 프로파일에서는 실행하지 않는다.
 */
@Component
@Profile("!test")
public class AdminSeeder implements DataSeeder {

    private static final String ADMIN_EMAIL = "admin@dongnemarket.com";
    private static final String ADMIN_RAW_PASSWORD = "admin1234!";
    private static final String ADMIN_NICKNAME = "관리자";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    @Transactional
    public void seed() {
        if (memberRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        memberRepository.save(Member.createAdmin(
                ADMIN_EMAIL,
                passwordEncoder.encode(ADMIN_RAW_PASSWORD),
                ADMIN_NICKNAME));
    }
}

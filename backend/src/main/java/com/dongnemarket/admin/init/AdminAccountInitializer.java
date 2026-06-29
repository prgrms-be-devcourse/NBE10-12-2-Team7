package com.dongnemarket.admin.init;

import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 관리자(ROLE_ADMIN) 계정을 1개 시드한다.
 * 같은 이메일 계정이 이미 있으면 만들지 않는다(멱등). 비밀번호는 BCrypt 로 인코딩해 저장한다.
 */
@Component
@Profile("!test")
public class AdminAccountInitializer implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@dongnemarket.com";
    private static final String ADMIN_RAW_PASSWORD = "admin1234!";
    private static final String ADMIN_NICKNAME = "관리자";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        memberRepository.save(Member.createAdmin(
                ADMIN_EMAIL,
                passwordEncoder.encode(ADMIN_RAW_PASSWORD),
                ADMIN_NICKNAME));
    }
}

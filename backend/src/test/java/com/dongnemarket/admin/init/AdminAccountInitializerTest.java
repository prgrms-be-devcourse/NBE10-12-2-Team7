package com.dongnemarket.admin.init;

import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AdminAccountInitializer adminAccountInitializer;

    @Test
    @DisplayName("관리자 계정이 없으면 ROLE_ADMIN 계정을 시드한다")
    void seedsAdminWhenNotExists() {
        given(memberRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded-pw");

        adminAccountInitializer.run(null);

        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("관리자 계정이 이미 있으면 시드하지 않는다(멱등)")
    void doesNotSeedWhenAlreadyExists() {
        given(memberRepository.existsByEmail(anyString())).willReturn(true);

        adminAccountInitializer.run(null);

        verify(memberRepository, never()).save(any(Member.class));
    }
}

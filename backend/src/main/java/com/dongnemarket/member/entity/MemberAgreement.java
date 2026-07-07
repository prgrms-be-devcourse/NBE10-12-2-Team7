package com.dongnemarket.member.entity;

import com.dongnemarket.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 회원가입 시 약관/개인정보 동의 이력 한 건. 회원당 동의 항목 수만큼 row가 쌓인다(1:N).
 * <p>약관 본문은 DB가 아니라 별도 정책 문서로 관리하고, 여기서는 어떤 버전에 언제 동의했는지 증적만 남긴다.
 */
@Entity
@Table(name = "member_agreements")
public class MemberAgreement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_type", nullable = false, length = 30)
    private AgreementType agreementType;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    protected MemberAgreement() {
    }

    private MemberAgreement(Member member, AgreementType agreementType, String version,
            LocalDateTime agreedAt, String ipAddress, String userAgent) {
        this.member = member;
        this.agreementType = agreementType;
        this.version = version;
        this.agreedAt = agreedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public static MemberAgreement of(Member member, AgreementType agreementType, String version,
            LocalDateTime agreedAt, String ipAddress, String userAgent) {
        return new MemberAgreement(member, agreementType, version, agreedAt, ipAddress, userAgent);
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public AgreementType getAgreementType() {
        return agreementType;
    }

    public String getVersion() {
        return version;
    }

    public LocalDateTime getAgreedAt() {
        return agreedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }
}

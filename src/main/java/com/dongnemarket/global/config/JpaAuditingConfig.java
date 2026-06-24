package com.dongnemarket.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 ({@link com.dongnemarket.global.common.BaseTimeEntity} 의 시각 자동 주입).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}

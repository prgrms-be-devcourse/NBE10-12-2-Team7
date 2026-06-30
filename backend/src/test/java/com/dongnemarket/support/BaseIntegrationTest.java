package com.dongnemarket.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합테스트 베이스: 진짜 MySQL(Testcontainers) 위에서 전체 컨텍스트를 띄운다.
 *
 * - integration 프로필 → CategoryInitializer/AdminAccountInitializer 가 부팅 시 시드(시드전략 1).
 *   (AdminAccountInitializer 는 @Profile("!test") 라 integration 에서는 그대로 동작)
 * - 각 테스트 종료 후 시나리오 테이블만 정리(시드 데이터는 보존).
 *
 * 실행: ./gradlew integrationTest  (Docker Desktop 필요)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Tag("integration")
@Testcontainers
@Sql(scripts = "/sql/clean-scenario.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withReuse(true);
}

package com.dongnemarket.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.global.config.JpaAuditingConfig;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportReason;
import com.dongnemarket.report.entity.ReportType;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * findAllByReporter의 N+1 방지(fetch join EntityGraph)를 실제 쿼리 통계로 검증한다.
 * targetProduct/targetMember 지연 로딩 필드를 신고 건수만큼 순회하며 읽어도,
 * 실행되는 SQL 문 개수가 신고 건수에 비례해 늘어나지 않아야 한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ReportRepositoryTest {

    @Autowired ReportRepository reportRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired RegionRepository regionRepository;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName("신고가 여러 건이어도 targetProduct/targetMember를 모두 읽는 데 쿼리 수가 늘어나지 않는다 (N+1 방지)")
    void findAllByReporter_doesNotCauseNPlusOne() {
        Member reporter = memberRepository.save(Member.createUser("reporter@example.com", "pw", "신고자"));
        Member seller = memberRepository.save(Member.createUser("seller@example.com", "pw", "판매자"));
        Member targetMember = memberRepository.save(Member.createUser("target@example.com", "pw", "신고대상"));
        Category category = categoryRepository.save(new Category("디지털기기"));
        Region region = regionRepository.save(new Region("1168010100", 3, null, "서울특별시 강남구 역삼동", "역삼동"));

        int productReportCount = 5;
        for (int i = 0; i < productReportCount; i++) {
            Product product = productRepository.save(Product.create(
                    seller, category, "상품 " + i, "설명", BigDecimal.valueOf(10000), region));
            reportRepository.save(Report.ofProduct(reporter, product, ReportReason.FAKE_ITEM, "신고 " + i));
        }
        reportRepository.save(Report.ofMember(reporter, targetMember, ReportReason.FRAUD_SUSPECTED, "회원 신고"));
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Report> reports = reportRepository.findAllByReporter(reporter);
        for (Report report : reports) {
            if (report.getReportType() == ReportType.PRODUCT) {
                report.getTargetProduct().getId();
            } else {
                report.getTargetMember().getId();
            }
        }

        long queryCount = statistics.getPrepareStatementCount();

        assertThat(reports).hasSize(productReportCount + 1);
        // EntityGraph 없이 지연 로딩만 썼다면 목록 조회 1 + 신고 건수(6)만큼의 추가 SELECT가 필요했을 것.
        // fetch join으로 신고 건수와 무관하게 쿼리 수가 일정하게(2건 이하) 유지되는지 확인한다.
        assertThat(queryCount).isLessThanOrEqualTo(2);
    }
}

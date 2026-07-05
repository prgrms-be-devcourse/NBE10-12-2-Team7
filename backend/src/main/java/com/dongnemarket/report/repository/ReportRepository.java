package com.dongnemarket.report.repository;

import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.report.entity.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterAndTargetProduct(Member reporter, Product targetProduct);

    boolean existsByReporterAndTargetMember(Member reporter, Member targetMember);

    /**
     * targetProduct/targetMember(둘 다 지연 로딩 @ManyToOne)를 한 번에 fetch join한다.
     * MyReportResponse::from이 신고마다 둘 중 하나를 읽는데, 이 EntityGraph 없이는
     * 신고 건수만큼 추가 SELECT가 발생하는 N+1이 생긴다.
     */
    @EntityGraph(attributePaths = {"targetProduct", "targetMember"})
    List<Report> findAllByReporter(Member reporter);
}

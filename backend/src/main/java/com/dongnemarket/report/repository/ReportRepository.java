package com.dongnemarket.report.repository;

import com.dongnemarket.member.entity.Member;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterAndTargetProduct(Member reporter, Product targetProduct);

    boolean existsByReporterAndTargetMember(Member reporter, Member targetMember);

    List<Report> findAllByReporter(Member reporter);
}

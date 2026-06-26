package com.dongnemarket.report.repository;

import com.dongnemarket.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetProductId(Long reporterId, Long targetProductId);

    boolean existsByReporterIdAndTargetMemberId(Long reporterId, Long targetMemberId);

    List<Report> findAllByReporterId(Long reporterId);
}

package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminMannerResponse;
import com.dongnemarket.manner.service.MannerScoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminMannerService {

    /** 기본 저신뢰 기준. 기본값(36.5)보다 한참 낮은 값을 기본 임계치로 둔다. */
    private static final BigDecimal DEFAULT_THRESHOLD = BigDecimal.valueOf(20.0);

    private final MannerScoreService mannerScoreService;

    public AdminMannerService(MannerScoreService mannerScoreService) {
        this.mannerScoreService = mannerScoreService;
    }

    public List<AdminMannerResponse> getLowTrustMembers(BigDecimal threshold) {
        BigDecimal effectiveThreshold = threshold != null ? threshold : DEFAULT_THRESHOLD;
        return mannerScoreService.findLowTrustMembers(effectiveThreshold).stream()
                .map(AdminMannerResponse::from)
                .toList();
    }
}

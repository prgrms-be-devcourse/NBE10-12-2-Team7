package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminMannerResponse;
import com.dongnemarket.manner.entity.MannerScore;
import com.dongnemarket.manner.service.MannerScoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminMannerService {

    private final MannerScoreService mannerScoreService;

    public AdminMannerService(MannerScoreService mannerScoreService) {
        this.mannerScoreService = mannerScoreService;
    }

    public List<AdminMannerResponse> getLowTrustMembers(BigDecimal threshold) {
        BigDecimal effectiveThreshold = threshold != null ? threshold : MannerScore.LOW_TRUST_THRESHOLD;
        return mannerScoreService.findLowTrustMembers(effectiveThreshold).stream()
                .map(AdminMannerResponse::from)
                .toList();
    }
}

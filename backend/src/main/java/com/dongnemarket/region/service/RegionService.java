package com.dongnemarket.region.service;

import java.util.List;

import com.dongnemarket.region.dto.RegionResponse;
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class RegionService {

	private final RegionRepository regionRepository;

	public RegionService(RegionRepository regionRepository) {
		this.regionRepository = regionRepository;
	}

	public List<RegionResponse> getRegions() {
		return getRegions(null);
	}

	public List<RegionResponse> getRegions(String parentCode) {
		if (!StringUtils.hasText(parentCode)) {
			return regionRepository.findAllByParentIsNullOrderByDisplayNameAsc()
					.stream()
					.map(RegionResponse::from)
					.toList();
		}

		return regionRepository.findAllByParentCodeOrderByDisplayNameAsc(parentCode)
				.stream()
				.map(RegionResponse::from)
				.toList();
	}
}

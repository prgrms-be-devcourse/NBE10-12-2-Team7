package com.dongnemarket.region.service;

import java.util.List;

import com.dongnemarket.region.dto.RegionResponse;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RegionService {

	private final RegionRepository regionRepository;

	public RegionService(RegionRepository regionRepository) {
		this.regionRepository = regionRepository;
	}

	// parentId가 없으면 최상위(시도) 목록, 있으면 해당 지역의 자식 목록을 반환한다.
	public List<RegionResponse> getRegions(Long parentId) {
		List<Region> regions = (parentId == null)
				? regionRepository.findByParentIsNullOrderByCodeAsc()
				: regionRepository.findByParentIdOrderByCodeAsc(parentId);
		return regions.stream()
				.map(RegionResponse::from)
				.toList();
	}
}

package com.dongnemarket.region.repository;

import java.util.List;
import java.util.Optional;

import com.dongnemarket.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

	// 최상위(시도) 목록. code 오름차순 안정 정렬.
	List<Region> findByParentIsNullOrderByCodeAsc();

	// 특정 지역의 자식 목록. code 오름차순.
	List<Region> findByParentIdOrderByCodeAsc(Long parentId);

	// 데모/테스트 편의: 특정 level의 첫 지역(code 오름차순).
	Optional<Region> findFirstByLevelOrderByCodeAsc(int level);
}

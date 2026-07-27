package com.dongnemarket.region.repository;

import java.util.List;
import java.util.Optional;

import com.dongnemarket.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RegionRepository extends JpaRepository<Region, Long> {

	boolean existsByName(String name);

	boolean existsByCode(String code);

	Optional<Region> findByCode(String code);

	Optional<Region> findByName(String name);

	List<Region> findAllByOrderByNameAsc();

	List<Region> findAllByParentIsNullOrderByDisplayNameAsc();

	List<Region> findAllByParentCodeOrderByDisplayNameAsc(String parentCode);

	List<Region> findAllByCodeIn(List<String> codes);

	long countByLevel(int level);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Region r where r.level = :level")
	void deleteByLevel(int level);
}

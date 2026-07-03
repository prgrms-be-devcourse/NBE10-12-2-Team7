package com.dongnemarket.region.repository;

import java.util.List;

import com.dongnemarket.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

	boolean existsByName(String name);

	List<Region> findAllByOrderByNameAsc();
}

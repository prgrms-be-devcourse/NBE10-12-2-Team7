package com.dongnemarket.member.repository;

import java.util.List;

import com.dongnemarket.member.entity.MemberLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberLocationRepository extends JpaRepository<MemberLocation, Long> {

	List<MemberLocation> findAllByMemberIdOrderBySortOrderAsc(Long memberId);

	List<MemberLocation> findAllByRegionRefIsNull();

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from MemberLocation ml where ml.member.id = :memberId")
	void deleteAllByMemberId(@Param("memberId") Long memberId);
}

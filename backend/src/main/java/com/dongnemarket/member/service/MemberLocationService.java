package com.dongnemarket.member.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.dto.MemberLocationResponse;
import com.dongnemarket.member.dto.MemberLocationUpdateRequest;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberLocation;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberLocationRepository;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class MemberLocationService {

	private final MemberRepository memberRepository;
	private final MemberLocationRepository memberLocationRepository;
	private final RegionRepository regionRepository;

	public MemberLocationService(MemberRepository memberRepository,
								 MemberLocationRepository memberLocationRepository,
								 RegionRepository regionRepository) {
		this.memberRepository = memberRepository;
		this.memberLocationRepository = memberLocationRepository;
		this.regionRepository = regionRepository;
	}

	@Transactional
	public List<MemberLocationResponse> updateMyLocations(Long memberId, MemberLocationUpdateRequest request) {
		Member member = getActiveMember(memberId);
		List<Region> regions = getRequiredDongRegions(request.getRegionCodes());

		memberLocationRepository.deleteAllByMemberId(memberId);
		List<MemberLocation> memberLocations = new ArrayList<>();
		for (int i = 0; i < regions.size(); i++) {
			memberLocations.add(MemberLocation.create(member, regions.get(i), i, i == 0));
		}

		return memberLocationRepository.saveAll(memberLocations).stream()
				.map(MemberLocationResponse::from)
				.toList();
	}

	public List<MemberLocationResponse> getMyLocations(Long memberId) {
		getActiveMember(memberId);
		return memberLocationRepository.findAllByMemberIdOrderBySortOrderAsc(memberId).stream()
				.map(MemberLocationResponse::from)
				.toList();
	}

	private Member getActiveMember(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		validateActiveMember(member);
		return member;
	}

	private void validateActiveMember(Member member) {
		if (member.getStatus() == MemberStatus.DELETED) {
			throw new BusinessException(ErrorCode.DELETED_MEMBER);
		}
		if (member.getStatus() == MemberStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.SUSPENDED_MEMBER);
		}
	}

	private List<Region> getRequiredDongRegions(List<String> regionCodes) {
		if (regionCodes == null || regionCodes.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}

		Set<String> uniqueRegionCodes = new HashSet<>(regionCodes);
		if (uniqueRegionCodes.size() != regionCodes.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}

		List<Region> regions = new ArrayList<>();
		for (String regionCode : regionCodes) {
			if (!StringUtils.hasText(regionCode)) {
				throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
			}
			Region region = regionRepository.findByCode(regionCode)
					.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
			if (region.getLevel() != 3) {
				throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
			}
			regions.add(region);
		}
		return regions;
	}
}

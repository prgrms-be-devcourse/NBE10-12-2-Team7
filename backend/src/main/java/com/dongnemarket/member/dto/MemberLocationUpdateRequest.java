package com.dongnemarket.member.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class MemberLocationUpdateRequest {

	@NotEmpty(message = "동네는 1개 이상 설정해야 합니다.")
	@Size(max = 2, message = "동네는 최대 2개까지 설정할 수 있습니다.")
	private List<@NotBlank(message = "동네 이름은 공백일 수 없습니다.") String> regions;

	protected MemberLocationUpdateRequest() {
	}

	public MemberLocationUpdateRequest(List<String> regions) {
		this.regions = regions;
	}

	public List<String> getRegions() {
		return regions;
	}
}

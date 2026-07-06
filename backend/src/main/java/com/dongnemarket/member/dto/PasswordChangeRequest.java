package com.dongnemarket.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PasswordChangeRequest {

	@NotBlank(message = "현재 비밀번호는 필수입니다.")
	private String currentPassword;

	@NotBlank(message = "새 비밀번호는 필수입니다.")
	@Size(min = 10, max = 64, message = "비밀번호는 10자 이상 64자 이하로 입력해주세요.")
	@Pattern(
			regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
			message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 하며 공백을 포함할 수 없습니다."
	)
	private String newPassword;

	protected PasswordChangeRequest() {
	}

	public PasswordChangeRequest(String currentPassword, String newPassword) {
		this.currentPassword = currentPassword;
		this.newPassword = newPassword;
	}

	public String getCurrentPassword() {
		return currentPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}
}

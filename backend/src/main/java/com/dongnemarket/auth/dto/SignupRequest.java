package com.dongnemarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequest {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 10, max = 64, message = "비밀번호는 10자 이상 64자 이하로 입력해주세요.")
	@Pattern(
			regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
			message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 하며 공백을 포함할 수 없습니다."
	)
	private String password;

	@NotBlank(message = "닉네임은 필수입니다.")
	@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
	private String nickname;

	/** 이용약관 동의 여부. 필수 동의 항목이라 서비스 레벨에서 검증한다(미동의 시 TERMS_NOT_AGREED). */
	private boolean termsAgreed;

	/** 개인정보 수집 및 이용 동의 여부. 필수 동의 항목이라 서비스 레벨에서 검증한다(미동의 시 PERSONAL_INFO_COLLECTION_NOT_AGREED). */
	private boolean personalInfoCollectionAgreed;

	protected SignupRequest() {
	}

	public SignupRequest(String email, String password, String nickname,
			boolean termsAgreed, boolean personalInfoCollectionAgreed) {
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.termsAgreed = termsAgreed;
		this.personalInfoCollectionAgreed = personalInfoCollectionAgreed;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public String getNickname() {
		return nickname;
	}

	public boolean isTermsAgreed() {
		return termsAgreed;
	}

	public boolean isPersonalInfoCollectionAgreed() {
		return personalInfoCollectionAgreed;
	}
}

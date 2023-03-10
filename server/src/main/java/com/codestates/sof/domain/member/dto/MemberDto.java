package com.codestates.sof.domain.member.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class MemberDto {
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Post {
		private String email;
		private String password;
		private String name;

	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Patch {
		private long memberId;
		private String name;
		// TODO : 비밀번호만 변경하는 로직이 따로 필요할까?
		private String password;

		// TODO : Profile 객체 필드값도 변경해야합니다.
		private ProfileDto profile;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Response {
		private long memberId;
		private String email;
		private String name;
		private boolean verificationFlag;
		private boolean deleteFlag;
		private LocalDateTime lastActivateAt;
		private ProfileDto profile;
	}
}
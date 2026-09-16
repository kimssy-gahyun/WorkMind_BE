package com.gh.workmind.member.model.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Setter
@Getter
@ToString
public class MemberDTO {

	private Long memberId;
	private String email;
	private String password;
	private String name;
	private String role;
	private LocalDateTime enrollDate;
	private LocalDateTime modifyDate;
	
}

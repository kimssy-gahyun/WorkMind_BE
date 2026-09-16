package com.gh.workmind.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gh.workmind.auth.jwt.JwtPrincipal;
import com.gh.workmind.member.model.dto.MemberDTO;
import com.gh.workmind.member.model.dto.MemberProfileResponse;
import com.gh.workmind.member.model.service.MemberService;

/*
 * * 회원 API
 * 
 * 회원 가입 (C)		POST		/members
 * 내 정보 조회 (R)	GET			/members/me
 */

@CrossOrigin(origins="http://localhost:5173")
@RestController
public class MemberController {
	
	@Autowired
	private MemberService memberService;
	
	/**
	 * 회원가입 Controller
	 * @param member 회원가입을 요청한 회원의 정보
	 * @return 회원가입 성공 시 201 Created 응답
	 */
	@PostMapping("/members")
	public ResponseEntity<Void> insertMember(@RequestBody MemberDTO member) {
		
		memberService.insertMember(member);
		
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}
	
	@GetMapping("/members/me")
	public ResponseEntity<MemberProfileResponse> selectMember(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.of(memberService.findCurrentMember(principal.memberId()));
    }
	
}

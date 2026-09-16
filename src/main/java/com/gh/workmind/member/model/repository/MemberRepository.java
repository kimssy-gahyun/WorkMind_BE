package com.gh.workmind.member.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gh.workmind.member.model.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	
	// Member save(Member) : 회원가입 시 사용

}

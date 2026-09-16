package com.gh.workmind.member.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gh.workmind.member.model.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);
	
	// Member save(Member) : 회원가입 시 사용

}

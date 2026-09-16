package com.gh.workmind.member.model.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gh.workmind.member.model.dto.MemberDTO;
import com.gh.workmind.member.model.entity.Member;
import com.gh.workmind.member.model.repository.MemberRepository;

@Service
public class MemberService {
	
	@Autowired
	private MemberRepository memberRepository;

	public void insertMember(MemberDTO member) {
		
		// DTO -> Entity 변환
		Member m = new Member();
		m.setEmail(member.getEmail());
		m.setPassword(member.getPassword());
		m.setName(member.getName());
		m.setRole(member.getRole());
		
		// insert
		memberRepository.save(m);
		
	}
	
	

}

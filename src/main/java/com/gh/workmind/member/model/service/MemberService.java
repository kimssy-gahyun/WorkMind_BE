package com.gh.workmind.member.model.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gh.workmind.member.model.dto.MemberDTO;
import com.gh.workmind.member.model.dto.MemberProfileResponse;
import com.gh.workmind.member.model.entity.Member;
import com.gh.workmind.member.model.repository.MemberRepository;

@Service
public class MemberService {
	
	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	public void insertMember(MemberDTO member) {
		
		// DTO -> Entity 변환
		Member m = new Member();
		m.setEmail(member.getEmail());
		m.setPassword(bCryptPasswordEncoder.encode(member.getPassword()));
		m.setName(member.getName());
		m.setRole(member.getRole());
		
		// insert
		memberRepository.save(m);
		
	}
	
	


    @Transactional(readOnly = true)
    public Optional<MemberProfileResponse> findCurrentMember(Long memberId) {
        return memberRepository.findById(memberId)
                .map(member -> new MemberProfileResponse(
                        member.getMemberId(),
                        member.getEmail(),
                        member.getName(),
                        member.getRole(),
                        member.getEnrollDate(),
                        member.getModifyDate()));
    }
}
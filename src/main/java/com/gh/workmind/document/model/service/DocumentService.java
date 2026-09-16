package com.gh.workmind.document.model.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gh.workmind.document.model.dto.DocumentDTO;
import com.gh.workmind.document.model.entity.Document;
import com.gh.workmind.document.model.repository.DocumentRepository;
import com.gh.workmind.member.model.entity.Member;
import com.gh.workmind.member.model.repository.MemberRepository;

@Service
public class DocumentService {

	@Autowired
	private DocumentRepository documentRepository;
	
	@Autowired
	private MemberRepository memberRepository;

	public void insertDocument(DocumentDTO document, Long memberId) {
		
		// 작성자 정보 불러오기
		Member member = memberRepository.findById(memberId)
						.orElseThrow(() -> 
							new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")
						);
		
		// DTO -> Entity 변환
		Document d = new Document();
		d.setTitle(document.getTitle());
		d.setContent(document.getContent());
		d.setOriginalFileName(document.getOriginalFileName());
		d.setStoredFileName(document.getStoredFileName());
		d.setFileSize(document.getFileSize());
		d.setFilePath(document.getFilePath());
		d.setMember(member); // 작성자 정보 그대로 넣기 (ManyToOne)
		
		documentRepository.save(d);
	}
	
	
	
}

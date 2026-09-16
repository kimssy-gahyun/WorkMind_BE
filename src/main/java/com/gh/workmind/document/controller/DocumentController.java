package com.gh.workmind.document.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gh.workmind.auth.jwt.JwtPrincipal;
import com.gh.workmind.common.template.FileRenamePolicy;
import com.gh.workmind.document.model.dto.DocumentDTO;
import com.gh.workmind.document.model.service.DocumentService;

import jakarta.servlet.http.HttpSession;


/*
 * * 문서 API
 * 
 * 문서 등록 (C)		POST		/documents
 * 문서 목록 조회 (R)	GET			/documents
 * 문서 상세 조회 (R)  GET			/documents/{documentId}
 * 문서 수정 (U)		PUT			/documents/{documentId}
 * 문서 삭제 (D) 		DELETE		/documents/{documentId}
 */

@CrossOrigin(origins="http://localhost:5173")
@RestController
public class DocumentController {
	
	@Autowired
	private DocumentService documentService;
	
	@PostMapping("/documents")
	public void insertDocument(@ModelAttribute DocumentDTO document, 
							   @RequestParam("upfile") MultipartFile upfile, 
							   HttpSession session,
							   Authentication authentication) {
		
		// 인증 토큰값으로부터 회원의 정보 가져오기
		JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
		
		// 넘어온 첨부파일이 없을 경우
	    if (upfile == null || upfile.isEmpty()) {
	    	
	        throw new IllegalArgumentException("첨부파일은 필수입니다.");
	    }
		
		// 첨부파일 처리
		String originalFileName = upfile.getOriginalFilename();
		String storedFileName = FileRenamePolicy.saveFile(upfile, session);
		
		document.setOriginalFileName(originalFileName);
		document.setStoredFileName(storedFileName);
		document.setFileSize(upfile.getSize());
		document.setFilePath("/resources/document_upfiles/");
		
		documentService.insertDocument(document, principal.memberId());
		
	}
	
	
	

}

package com.gh.workmind.document.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gh.workmind.document.model.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	// Document save(Document) : 문서 등록 시 사용
	
}

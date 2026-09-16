package com.gh.workmind.document.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import com.gh.workmind.member.model.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="DOCUMENT")
@DynamicInsert

@NoArgsConstructor
@Setter
@Getter
public class Document {

	@Id
	@Column(name="DOCUMENT_ID")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long documentId;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="MEMBER_ID")
	private Member member;
	
	@Column(name="TITLE")
	private String title;
	
	@Column(name="CONTENT")
	private String content;
	
	@Column(name="ORIGINAL_FILE_NAME")
	private String originalFileName;
	
	@Column(name="STORED_FILE_NAME")
	private String storedFileName;
	
	@Column(name="FILE_PATH")
	private String filePath;
	
	@Column(name="FILE_SIZE")
	private Long fileSize;
	
	@Column(name="ENROLL_DATE")
	private LocalDateTime enrollDate;

	@Column(name="MODIFY_DATE")
	private LocalDateTime modifyDate;
	
}

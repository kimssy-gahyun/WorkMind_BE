package com.gh.workmind.document.model.dto;

import java.time.LocalDateTime;

import com.gh.workmind.member.model.entity.Member;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Setter
@Getter
@ToString
public class DocumentDTO {

	private Long documentId;
	private Long memberId;
	private String memberName; // 작성자 조회용
	private String title;
	private String content;
	private String originalFileName;
	private String storedFileName;
	private String filePath;
	private Long fileSize;
	private LocalDateTime enrollDate;
	private LocalDateTime modifyDate;
	
}

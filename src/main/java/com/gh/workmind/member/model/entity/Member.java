package com.gh.workmind.member.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name="MEMBER")
@DynamicInsert

@NoArgsConstructor
@Setter
@Getter
@ToString
public class Member {

	@Id
	@Column(name="MEMBER_ID")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long memberId;
	
	@Column(name="EMAIL")
	private String email;
	
	@Column(name="PASSWORD")
	private String password;
	
	@Column(name="NAME")
	private String name;
	
	@Column(name="ROLE")
	private String role;
	
	@Column(name="ENROLL_DATE")
	private LocalDateTime enrollDate;
	
	@Column(name="MODIFY_DATE")
	private LocalDateTime modifyDate;
	
}






# WorkMind_BE

WorkMind의 백엔드 프로젝트입니다.

## 프로젝트 개요

WorkMind는 업무 중 생성되는 문서를 관리하고,
필요한 정보를 검색하고 활용할 수 있도록 만든 업무 지원 서비스입니다.

문서 관리 기능을 기반으로 LLM을 활용한 문서 분석과
RAG 기반 지식 검색 및 질의응답 기능을 제공합니다.

## 주요 기능

- 회원 및 권한 관리
- 업무 문서 등록 / 조회 / 수정 / 삭제
- 문서 내용 요약 및 주요 정보 추출
- RAG 기반 지식 검색 및 질의응답
- 답변에 활용된 참고 문서 확인
- 사용자 질의 이력 관리

## 기술 스택

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- MySQL
- Gradle
- Lombok

## 개발 환경

- Server Port: 8080
- Context Path: `/workmind`
- Database: MySQL

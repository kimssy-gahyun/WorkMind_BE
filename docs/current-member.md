# 현재 회원 정보 조회 — JWT 인증 4단계

## 변경 파일

- member/controller/MemberController.java: 기존 GET /members/me 빈 메서드 구현.
- member/model/service/MemberService.java: 읽기 전용 findCurrentMember(memberId) 추가.
- member/model/dto/MemberProfileResponse.java: password가 없는 응답 전용 record 추가.
- src/test/java/com/gh/workmind/member/controller/MemberControllerTest.java:
  실제 JWT Filter, SecurityConfig, Controller, Service와 mock Repository를 사용하는 테스트.
- 이 문서와 jwt-request-authentication.md: 현재 응답 및 Postman 절차 안내.

## 처리 흐름

Authorization: Bearer 토큰 → 기존 JWT Filter 검증 → SecurityContext 등록
→ @AuthenticationPrincipal JwtPrincipal 주입 → principal.memberId()
→ MemberRepository.findById(memberId) → MemberProfileResponse → 200 OK.

@AuthenticationPrincipal은 SecurityContext의 Authentication.getPrincipal()을 읽는다.
이메일이나 요청 파라미터로 회원을 선택하지 않는다.
Repository의 기존 JpaRepository.findById()를 사용하므로 추가 조회 메서드는 필요 없다.
서비스는 @Transactional(readOnly = true)로 실행한다.

응답의 memberId, email, name, role, enrollDate, modifyDate는 모두 조회한 Member에서 가져온다.
JWT의 email/role은 응답 정보로 사용하지 않는다.
기존 MemberDTO와 엔티티를 반환하지 않고 비밀번호 필드 자체가 없는 DTO로 변환한다.

서비스 조회 결과가 Optional.empty()이면 ResponseEntity.of()가 빈 404를 반환한다.
토큰이 없거나 유효하지 않거나 만료된 경우 기존 Security 설정이 401로 처리한다.
이 경우 회원 조회는 실행되지 않는다.

회원가입, 로그인, JWT 발급·필터, SecurityConfig, Gradle 및 외부 Secret 설정은 유지한다.
회원 수정·탈퇴, Refresh Token, 로그아웃, 토큰 저장 등은 추가하지 않는다.

## Postman

STS 프로젝트를 F5로 새로고침하고 필요하면 Project > Clean 후 서버를 재시작한다.
baseUrl: http://localhost:8080/workmind

### A. 로그인

POST {{baseUrl}}/auth/login
Authorization: No Auth
Content-Type: application/json

```json
{
  "email": "admin@workmind.com",
  "password": "1234"
}
```

DB에 해당 자격정보의 회원이 있을 때 200과 accessToken을 반환한다.
별도의 관리자 계정이나 샘플 데이터를 생성하지 않았다.

Post-response 스크립트:

```javascript
if (pm.response.code === 200) {
  pm.environment.set("accessToken", pm.response.json().accessToken);
}
```

### B. 정상 토큰으로 조회

GET {{baseUrl}}/members/me
Authorization 탭: Bearer Token
Token: {{accessToken}}

예상: 200 OK. 다음은 예시이며 실제 DB 값이 반환된다.

```json
{
  "memberId": 1,
  "email": "admin@workmind.com",
  "name": "관리자",
  "role": "ADMIN",
  "enrollDate": "2026-09-16T10:00:00",
  "modifyDate": null
}
```

응답에 password 필드가 없는지 확인한다.

### C. 토큰 없이 조회

같은 GET 요청에서 Authorization을 No Auth로 변경하고,
Headers에 직접 추가한 Authorization도 제거한다.
예상: 401 Unauthorized, 회원 정보 없음.

### D. 잘못된 토큰

Bearer Token 값을 invalid-token으로 변경한다.
예상: 401 Unauthorized, 회원 정보 없음. 500이 발생하지 않는다.

만료된 토큰도 401이다. 유효한 토큰이라도 해당 memberId의 DB 회원이 없으면 404다.
없는 회원 시나리오는 자동 테스트에서 검증하며 실제 회원을 삭제할 필요가 없다.

## 검증

프로젝트 루트에서 gradlew.bat test 실행.

전체 43개 테스트 통과:
- JWT 인증 및 기존 회원가입·로그인 회귀 테스트
- /members/me 정상 응답과 정확한 응답 필드, 비밀번호 비노출
- JWT와 다른 DB 최신 이메일·역할 반환
- 쿼리 파라미터 memberId로 다른 회원을 선택할 수 없음
- modifyDate가 null인 경우와 값이 있는 경우
- DB 회원 없음: 404
- 미인증·잘못된 토큰·만료 토큰: 401, Repository 호출 없음

회원 조회 테스트의 Repository는 mock이다.
기존 애플리케이션 기동 테스트는 설정된 MySQL을 사용한다.
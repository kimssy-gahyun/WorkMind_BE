# JWT 요청 인증 — 3단계

## 변경 파일

- `auth/jwt/JwtAuthenticationFilter.java`: OncePerRequestFilter 기반 Bearer 토큰 인증.
- `auth/jwt/JwtPrincipal.java`: 인증된 회원의 memberId, email, role 보관.
- `config/SecurityConfig.java`: stateless 설정, 필터 등록, 401/403 응답.
- `auth/jwt/JwtAuthenticationFilterTest.java`: 요청 인증과 공개 API 접근 테스트.
- `auth/controller/AuthControllerTest.java`: 새로운 Security 정책과 로그인 회귀 테스트.
- `docs/jwt-login.md`, 이 문서: 현재 인증 흐름과 사용 방법.

기존 JJWT 0.13.0과 JwtTokenProvider.parseAndValidate()를 재사용한다.
build.gradle, application.properties, 회원가입·로그인 운영 코드 및 토큰 발급 코드는 변경하지 않는다.

## 처리 흐름

1. Authorization 헤더가 없거나 Bearer 방식이 아니면 다음 필터로 진행한다.
2. Bearer 토큰을 추출하여 서명, 만료시간, 형식, 필수 claim을 검증한다.
3. JwtPrincipal(memberId, email, role)을 만든다.
4. UsernamePasswordAuthenticationToken.authenticated()로 인증 객체를 만든다.
5. SecurityContextHolder.getContext().setAuthentication()에 등록한다.
6. 다음 필터에서 접근 권한을 판단하고 컨트롤러로 진행한다.

principal에는 memberId, email, role이 저장된다. getName()은 email을 반환한다.
credentials는 null이며 비밀번호나 원문 Access Token을 인증 객체에 보관하지 않는다.
USER는 SimpleGrantedAuthority("ROLE_USER"), ADMIN은
SimpleGrantedAuthority("ROLE_ADMIN")으로 변환한다.
유효하지 않은 회원 식별자, 빈 이메일, 지원하지 않는 role은 인증하지 않는다.

DB 조회는 하지 않는다. 요청이 끝나면 Spring Security가 컨텍스트를 정리하며,
다음 요청에는 토큰을 다시 보내야 한다.

## Security 설정과 예외 처리

- CSRF, formLogin, httpBasic, 기본 logout, requestCache 비활성화.
- SessionCreationPolicy.STATELESS.
- POST /members, POST /auth/login만 permitAll.
- 그 외 요청은 authenticated.
- UsernamePasswordAuthenticationFilter 앞에 JWT 필터 등록.
- 필터는 SecurityFilterChain 안에서만 생성하여 서블릿 필터 중복 등록을 피한다.
- JwtException 및 IllegalArgumentException은 토큰 처리 범위에서 잡고 컨텍스트를 지운다.
- 잘못된 토큰도 다음 필터로 전달한다. 공개 API는 접근 가능하다.
- 보호된 API의 미인증 요청은 AuthenticationEntryPoint에서 401과
  WWW-Authenticate: Bearer를 반환한다.
- 인증 후 권한 부족은 AccessDeniedHandler에서 403을 반환한다.
- 컨트롤러에서 발생한 일반 오류를 JWT 오류로 처리하지 않는다.

Refresh Token, 로그아웃, 토큰 저장·블랙리스트, UserDetailsService,
ADMIN 전용 API는 추가하지 않는다. 회원 정보 조회는 4단계에서 구현되었으며 [현재 회원 조회](current-member.md)를 참고한다.

## Postman 테스트

서버를 재시작하고 baseUrl을 http://localhost:8080/workmind 로 설정한다.
STS에서는 프로젝트 Refresh(F5), 필요하면 Project > Clean 후 재실행한다.

### A. 토큰 없이 공개 API 호출

Authorization 탭을 No Auth로 설정한다.

POST {{baseUrl}}/members
Content-Type: application/json

```json
{
  "email": "jwt-step3-test@example.com",
  "password": "Test-password-123!",
  "name": "JWT 테스트",
  "role": "USER"
}
```

아직 가입하지 않은 이메일로 호출하면 기존 회원가입 로직에 따라 201을 반환한다.
이 요청은 실제 DB에 회원을 생성한다.

POST {{baseUrl}}/auth/login
Content-Type: application/json

```json
{
  "email": "jwt-step3-test@example.com",
  "password": "Test-password-123!"
}
```

200과 accessToken을 확인한다. 잘못된 자격정보는 기존처럼 401이다.
로그인 요청의 Post-response 스크립트에 다음을 넣으면 저장할 수 있다.

```javascript
if (pm.response.code === 200) {
  pm.environment.set("accessToken", pm.response.json().accessToken);
}
```

### B. 토큰 없이 보호된 경로 호출

GET {{baseUrl}}/members/me
Authorization: No Auth

예상: 401, WWW-Authenticate: Bearer.

4단계에서 /members/me가 구현되었다. 현재 동작은 [현재 회원 조회](current-member.md)를 참고한다.

### C. 정상 Access Token

같은 요청의 Authorization 탭에서 Bearer Token을 선택하고
Token에 {{accessToken}}만 입력한다. Bearer 접두사는 Postman이 붙인다.

예상: 200과 DB에서 조회한 현재 회원 정보. 해당 회원이 없으면 404를 반환한다.

응답은 DB에서 조회한 회원 정보이며 SecurityContext 객체 자체를 노출하지 않는다.
확인하려면 JwtAuthenticationFilter의 setAuthentication 이후에 중단점을 설정하고
authentication의 principal과 authorities를 확인한다.
자동 테스트는 테스트 전용 컨트롤러에서 SecurityContext를 직접 읽어 검증한다.
해당 테스트 경로는 실제 서버에는 등록되지 않는다.

### D. 잘못된 토큰

Token 값을 invalid-token으로 바꾸고 같은 보호 경로를 요청한다.
예상: 401. JWT 형식 오류로 500이 발생하지 않는다.
잘못된 토큰으로 POST /auth/login을 호출하더라도 정상 자격정보라면 로그인 가능하다.

### E. 만료된 토큰

로컬 실행 설정에 JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=5를 지정하고 서버를 재시작한다.
새로 로그인해 발급받은 토큰으로 6초 이상 지난 뒤 보호 경로를 요청한다.
예상: 401.

확인 후 만료시간 환경변수를 제거하거나 1800으로 복구하고 재시작한다.
이미 발급된 토큰의 exp는 설정 변경으로 바뀌지 않는다.

## 검증

프로젝트 루트에서 `gradlew.bat test`를 실행한다.
3단계 완료 시 전체 35개 테스트가 통과했다. 4단계 검증 결과는 [현재 회원 조회](current-member.md)를 참고한다. 필터 테스트는 Repository를 mock으로 대체하며,
정상 토큰 인증 과정에서 Repository 호출이 없음을 검증한다.
기존 애플리케이션 기동 테스트는 설정된 MySQL 연결을 사용한다.
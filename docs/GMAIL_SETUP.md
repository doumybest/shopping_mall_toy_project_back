# Gmail 회원가입 인증 설정

회원가입 순서: 이메일 입력 → 인증번호 발송 → 메일의 숫자 6자리 입력 → 인증 완료 → 이름/비밀번호 입력 → 회원가입.

## 1. Gmail 발신 계정 준비

1. 발신용 Google 계정에서 **2단계 인증**을 켭니다.
2. https://myaccount.google.com/apppasswords 에서 앱 비밀번호를 만듭니다(예: 소소마켓 개발).
3. 생성한 앱 비밀번호를 IntelliJ 환경변수에 설정합니다. Google 로그인 비밀번호를 사용하지 않습니다.
4. 앱 비밀번호를 소스·README·Git·채팅에 저장하지 마세요. 이미 노출했다면 Google에서 폐기하고 다시 만드세요.

회사/학교 Google 계정은 관리자 정책에 따라 앱 비밀번호를 사용할 수 없을 수 있습니다.
계정에서 앱 비밀번호 발급이 허용되지 않으면 OAuth 기반 메일 발송 또는 별도 SMTP 서비스를 사용해야 합니다. 이 구현은 SMTP 앱 비밀번호 방식입니다.

공식 안내:
- https://support.google.com/accounts/answer/185833?hl=ko
- https://developers.google.com/workspace/gmail/imap/imap-smtp

## 2. IntelliJ 환경변수 설정

Run → Edit Configurations → ShoppingMallApplication → Environment variables에서 아래 값을 입력하고 백엔드를 다시 실행합니다.
기존 DB/관리자 환경변수는 유지하세요.

| 변수 | 값 |
|---|---|
| MAIL_HOST | `smtp.gmail.com` |
| MAIL_PORT | `587` |
| MAIL_USERNAME | 발신용 Gmail 주소 |
| MAIL_PASSWORD | Google에서 발급한 앱 비밀번호(표시용 공백 제거) |
| MAIL_FROM | MAIL_USERNAME과 동일한 Gmail 주소 |

기본 SMTP 인증 및 STARTTLS는 켜져 있으며 서버 인증서 확인과 연결/읽기/쓰기 타임아웃이 설정되어 있습니다.
메일 비밀번호는 프론트엔드 VITE 환경변수에 넣지 않습니다.
`demo` 프로필도 실제 SMTP 설정이 있어야 가입 메일이 발송됩니다. 고정 코드, 콘솔 출력, 인증 우회는 없습니다.
`.env` 파일은 Spring Boot가 자동으로 읽지 않습니다. IntelliJ 실행 환경변수에 직접 입력하세요.

## 3. 기존 Oracle DB 변경

이미 이전 버전의 쇼핑몰 테이블을 만들었다면 MALL 계정 접속에서
`src/main/resources/db/migrations/V2__email_verification_oracle.sql`을 F5로 한 번 실행합니다.
DB 계정 생성/접속 서비스명은 기존 README를 참고하세요.

아직 테이블을 만들지 않은 새 DB라면 최신 `schema-oracle.sql`만 실행하면 됩니다. V2를 중복 실행하지 마세요.
H2 demo 파일 DB는 시작 시 새 테이블과 컬럼이 자동으로 추가됩니다. 기존 계정과 주문은 보존됩니다.

추가 데이터:
- `MALL_EMAIL_VERIFICATIONS`: 이메일별 인증번호 BCrypt 해시, 세션 해시, 만료·인증·사용 시각, 시도/발송 횟수
- `MALL_EMAIL_SEND_LIMITS`: 발송 요청 IP의 SHA-256 해시와 시간당 횟수(원본 IP는 저장하지 않음)
- `MALL_MEMBERS.EMAIL_VERIFIED_AT`: 신규 회원의 이메일 인증 일시

인증번호 원문은 DB·응답·로그에 저장하지 않습니다. 메일 본문에만 전달됩니다.
기존 회원/환경변수로 생성한 관리자는 가입 인증의 소급 적용 대상이 아닙니다. 기존 로그인은 유지됩니다.

## 4. 정책

| 항목 | 제한 |
|---|---|
| 인증번호 | SecureRandom으로 생성한 숫자 6자리 |
| 인증번호 유효시간 | 5분 |
| 재발송 | 마지막 발송 후 60초 |
| 이메일별 발송 | 1시간 최대 5회 |
| 요청 IP별 발송 | 1시간 최대 20회(한도에 걸린 요청도 집계될 수 있음) |
| 번호 입력 | 발송당 최대 5회, 틀린 횟수는 DB에 보존 |
| 인증 후 가입 | 같은 이메일·같은 브라우저 세션에서 10분 이내 1회 |

재발송 시 이전 번호와 인증 완료 상태를 폐기합니다. 이메일을 바꾸면 화면의 인증도 초기화됩니다.
SMTP 발송 실패 시 사용할 수 있는 인증번호를 남기지 않으며 가입을 허용하지 않습니다.
미인증·다른 이메일·다른 브라우저·만료·이미 사용한 인증 정보로 가입할 수 없습니다.
가입 저장에 실패하면 인증 정보 소비도 롤백되므로 유효시간 내 수정 후 재시도할 수 있습니다.
서버 재시작으로 세션이 바뀐 경우 다시 인증해야 합니다.

현재는 단일 서버의 실제 요청 IP를 기준으로 제한합니다. 프록시 뒤에서는 전체 트래픽이 하나의 IP로 집계될 수 있으므로 운영 프록시 신뢰 설정과 한도를 검토하세요.
만료된 인증/발송 제한 기록은 감사·개발 확인용으로 남습니다. 운영 전에는 보존 기간을 정하고 배치로 삭제해야 합니다.

## 5. 발송 확인

1. 백엔드 재시작 후 front에서 `pnpm dev` 실행
2. 회원가입 → 실제 수신 가능한 이메일 입력 → 인증번호 보내기
3. 수신함/스팸함에서 메일 확인 → 숫자 6자리 입력 → 인증번호 확인
4. 인증 완료 상태에서 회원가입
5. SQL Developer에서 아래 확인

```sql
SELECT email, email_verified_at FROM mall_members ORDER BY id DESC;
SELECT email, expires_at, verified_at, consumed_at, send_count, attempt_count
FROM mall_email_verifications;
```

- 발송 설정이 준비되지 않음: MAIL_HOST/MAIL_FROM 등 환경변수와 백엔드 재시작 확인
- 메일 발송 실패: Gmail 앱 비밀번호, 계정 정책, 외부 587 포트 연결 확인
- 429: 재발송/시간당 제한 또는 번호 오입력 한도. 안내 시간 후 재시도
- 인증 테이블이 없다는 DB 오류: Oracle V2 SQL 실행 확인

## 검증 범위

자동화 테스트는 외부 수신자에게 메일을 보내지 않고 loopback SMTP에서 실제 JavaMail SMTP 통신을 검증합니다.
브라우저 테스트도 테스트 실행기에서만 띄우는 로컬 메일함을 사용합니다. 운영 백엔드에 인증번호 조회 API는 없습니다.
Gmail 실제 수신은 발신 계정 앱 비밀번호를 로컬 설정한 뒤 별도 확인해야 합니다.

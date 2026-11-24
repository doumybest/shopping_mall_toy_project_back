# 소소마켓 백엔드
Java 21 · Spring Boot 4.1.2 · Spring Security · Spring Data JPA / Hibernate · Oracle.
프론트엔드는 `../front`의 Vue 3 앱이며 `/api` 요청을 8080으로 전달합니다.

## JPA 구조

`Controller → Service → Repository → Entity → DB` 구조입니다.
11개 테이블을 각각 Entity와 JpaRepository에 매핑했습니다. 운영 코드의 JdbcTemplate·RowMapper·Database 헬퍼를 제거했으며 Service에는 업무 규칙과 트랜잭션을 둡니다.
`ProductEntity`는 DB 매핑, `Product` record는 API 응답용 DTO입니다. Entity를 직접 JSON 응답으로 반환하지 않습니다.
기존 API와 Oracle 테이블은 그대로 사용합니다. JPA 전환만을 위한 추가 SQL은 필요하지 않습니다.
`ddl-auto=validate`로 기존 스키마만 검사하며 자동 생성·삭제·변경하지 않습니다. 최초 테이블 생성 및 버전 변경용 SQL 파일은 계속 사용합니다.
자세한 코드 읽기 순서는 [JPA 구조 안내](docs/JPA_STRUCTURE.md)를 참고하세요.

## 구현 기능

- 이메일 회원가입, BCrypt 비밀번호 해시, 세션 로그인/로그아웃, CSRF 방어
- Gmail/SMTP 인증번호 발송·확인 후 회원가입, 만료·재발송·오입력 제한
- 일반 회원/관리자 분리: 관리자만 상품 등록·수정·판매 중단 가능
- 계정별 DB 장바구니와 찜 목록
- 배송지 입력 → 주문 → 모의 결제 → 재고 차감 → 포인트 사용/1% 적립
- 주문 조회, 전체 취소, 모의 환급, 포인트 사용 반환/적립 회수
- 구매자 리뷰 작성 및 본인 리뷰 삭제
- 중복 주문 키, 주문 소유권 검사, 행 잠금을 통한 동시 주문 재고 보호

**결제는 MOCK 전용입니다. 실제 카드 결제·PayPay·토스페이먼츠를 호출하지 않으며 카드 번호를 수집하지 않습니다.**
회원/상품/장바구니/찜/주문/주문상품/결제/포인트 원장/리뷰를 DB에 저장합니다. 세션은 서버 메모리이므로 서버 재시작 시 다시 로그인해야 합니다.

## 1. Oracle 계정 만들기

기존 쇼핑몰 DB가 있다면 회원가입 인증 업데이트를 위해 `src/main/resources/db/migrations/V2__email_verification_oracle.sql`을 MALL 계정에서 한 번 실행하세요. 새 DB에는 최신 schema-oracle.sql만 실행합니다.
실제 메일 발송 설정은 [Gmail 인증 설정](docs/GMAIL_SETUP.md)을 참고하세요. SMTP 설정이 없으면 신규 회원가입을 완료할 수 없습니다.

기존 SYSTEM 계정에 앱 테이블을 만들지 않습니다. Oracle Free의 앱용 PDB인 **FREEPDB1**에서 별도 `mall` 계정을 사용합니다.
이전 연결 예시의 `FREE`는 루트 서비스입니다. 애플리케이션의 기본 URL은 `FREEPDB1`입니다.
설치 환경에 따라 서비스명이 다르면 해당 서비스명으로 변경하세요.

SQL Developer에서 SYSTEM으로 `localhost:1521/FREEPDB1`에 접속합니다.
다음 예시에서 비밀번호를 본인이 정한 값으로 바꿔 실행하세요. 스크립트에 실제 비밀번호를 저장하거나 Git에 커밋하지 마세요.

```sql
SELECT SYS_CONTEXT('USERENV', 'CON_NAME') AS container_name FROM dual;
-- FREEPDB1인지 확인한 뒤, MALL 계정이 아직 없을 때 한 번만 실행
CREATE USER mall IDENTIFIED BY "ReplaceWithYourOwnPassword"
  DEFAULT TABLESPACE users QUOTA 100M ON users;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO mall;
```

`mall`로 새 접속을 만들고 **동일한 FREEPDB1 서비스**에 연결합니다.
`src/main/resources/db/schema-oracle.sql` 파일을 열어 `mall` 접속에서 **F5**로 한 번 실행합니다.
자동 DROP이나 테이블 재생성은 하지 않습니다. 이 스크립트는 기존 테이블이 없는 최초 설치용입니다.

관련 공식 문서: https://docs.oracle.com/en/database/oracle/oracle-database/26/dbseg/creating-user-accounts.html

## 2. IntelliJ 실행 설정

1. `back/pom.xml`을 Maven 프로젝트로 열고 Reload All Maven Projects 실행
2. Project SDK 및 실행 JRE를 **JDK 21**로 지정
3. Run → Edit Configurations → ShoppingMallApplication → Environment variables에 아래 항목 입력

| 환경변수 | 값 |
|---|---|
| DB_URL | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| DB_USERNAME | `mall` |
| DB_PASSWORD | 생성한 mall DB 계정의 비밀번호 |
| MALL_ADMIN_EMAIL | 직접 정한 관리자 로그인 이메일 |
| MALL_ADMIN_PASSWORD | 직접 정한 관리자 로그인 비밀번호(10~64자, UTF-8 최대 72바이트) |

4. Active profiles는 비워 둡니다(기본 Oracle 모드).
5. ShoppingMallApplication.main 실행
6. http://localhost:8080/api/health 확인 후 front에서 `pnpm dev` 실행

DB 계정 비밀번호와 쇼핑몰 로그인 비밀번호는 서로 다른 정보입니다. 프론트의 VITE 환경변수에 DB 비밀번호를 넣지 마세요.
관리자 환경변수는 해당 이메일이 없을 때만 계정을 만듭니다. 기존 비밀번호는 덮어쓰지 않습니다.
일반 회원의 이메일을 지정해도 관리자 승격은 하지 않고 시작을 중단합니다. 새 관리자 이메일을 사용하세요.
최초 생성 후 관리자 환경변수를 제거해도 계정은 DB에 보존됩니다.
최초 빈 상품 테이블에는 샘플 상품 6개가 생성됩니다.

## Oracle 연결 전 빠른 확인

IntelliJ 실행 설정의 Active profiles를 `demo`로 지정합니다. DB 환경변수는 필요 없습니다.
관리자 계정이 필요하면 위의 MALL_ADMIN_EMAIL/PASSWORD만 설정합니다.

- `demo`: H2 파일 DB `back/data/mall`에 보존. Oracle 데이터와 별개이며 자동 이관하지 않습니다.
- `test`: H2 메모리 DB, 자동화 테스트 전용. 설정된 테스트 관리자 비밀번호를 실제 환경에서 사용하지 마세요.
- 기본: Oracle, 수동 최초 스키마 생성 필요

Maven이 PATH에 있다면 back에서:

```powershell
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Maven이 PATH에 없어도 IntelliJ의 내장 Maven으로 test와 spring-boot:run을 실행할 수 있습니다. Maven Wrapper는 포함하지 않았습니다.

## 포인트·주문 정책

- 금액은 정수 JPY 고정, 1 P = 1엔, 포인트 사용 상한은 보유 양수 잔액과 상품 합계 중 작은 값입니다. 통화 전환 또는 통화별 구매 제한은 없습니다.
- 적립은 포인트 차감 후 모의 결제액의 1%, 소수점 버림
- 배송비는 무료, 장바구니 전체를 한 번에 주문. 장바구니 가격은 예약되지 않으며 결제 시 DB의 최신 가격/재고 적용
- 요청마다 requestKey(UUID)를 사용. 동일 회원/키 재시도는 기존 주문 반환. 내용이 달라지면 409
- 결제와 주문·상품 스냅샷·재고·포인트 원장·장바구니 비우기는 동일 트랜잭션
- 취소는 주문 전체에 대해 한 번만 정산. 실배송 처리/부분 취소는 없음
- 적립 포인트를 다른 주문에서 쓴 후 원래 주문을 취소하면 잔액이 음수가 될 수 있음. 이후 적립으로 상계하며 양수 잔액만 사용 가능
- 삭제 상품은 active=0으로 보관하여 주문 이력 유지. 과거 주문에는 당시 상품명과 가격 보존
- 리뷰는 구매 완료한 상품에 회원당 1개, 본인 삭제 가능. 작성 후 주문 취소해도 기존 리뷰는 유지

## 구조

| 패키지 | 역할 |
|---|---|
| auth | 가입, 세션 인증, 비밀번호, 현재 회원 |
| product | ProductEntity / ProductRepository / ProductService, 상품 조회 및 관리자 CRUD |
| cart | DB 장바구니와 찜 |
| order | 주문, 모의 결제, 재고, 포인트 원장, 취소 |
| review | 구매자 리뷰 |
| common | Entity 공통 ID/생성 시각, 복합키, 오류 응답, 최초 데이터 |

API와 DB 구조는 `docs/ARCHITECTURE.md`를 참고하세요.

## 검증 및 범위

통합 테스트는 H2 Oracle 호환 모드에서 회원 권한/CSRF/중복 주문/동시 재고/트랜잭션 롤백/포인트 정산/리뷰 권한을 검증합니다.
테스트 코드의 JdbcTemplate은 독립적인 DB 결과 확인과 실패 주입에만 사용합니다. 실제 서비스의 DB 접근은 Repository가 담당합니다.
실제 Oracle 접속 검증은 별도로 필요합니다. H2 통과가 Oracle 접속 성공을 뜻하지 않습니다.
현재 로컬 학습용 MVP입니다. 대규모 목록 페이지네이션, 비밀번호 복구, 로그인 시도 제한, 배송 운영, 이미지 업로드, 실제 PG 승인·웹훅·환불 대사는 후속 작업입니다.
운영 배포 시 HTTPS, COOKIE_SECURE=true, 동일 출처 /api 프록시, 운영 비밀 관리가 필요합니다.
Spring Security CSRF 참고: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html

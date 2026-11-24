-- 기존 쇼핑몰 테이블이 있는 MALL 계정에서 F5로 한 번만 실행합니다.
-- 신규 설치(schema-oracle.sql 실행)에는 이 파일을 추가 실행하지 않습니다.
ALTER TABLE mall_members ADD email_verified_at TIMESTAMP;
CREATE TABLE mall_email_verifications (
 email VARCHAR2(200 CHAR) PRIMARY KEY,
 code_hash VARCHAR2(100 CHAR),
 session_hash VARCHAR2(64 CHAR),
 expires_at TIMESTAMP,
 verified_at TIMESTAMP,
 consumed_at TIMESTAMP,
 sent_at TIMESTAMP,
 window_started_at TIMESTAMP NOT NULL,
 send_count NUMBER(10) DEFAULT 0 NOT NULL CHECK (send_count >= 0),
 attempt_count NUMBER(10) DEFAULT 0 NOT NULL CHECK (attempt_count BETWEEN 0 AND 5)
);
CREATE TABLE mall_email_send_limits (
 source_hash VARCHAR2(64 CHAR) PRIMARY KEY,
 window_started_at TIMESTAMP NOT NULL,
 send_count NUMBER(10) DEFAULT 0 NOT NULL CHECK (send_count >= 0)
);

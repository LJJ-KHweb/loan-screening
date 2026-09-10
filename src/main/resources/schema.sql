-- 여신 심사 이력 테이블
-- SQL Developer 에서 접속 계정으로 한 번 실행한다
-- 다시 만들 때는 아래 DROP 두 줄을 먼저 실행

-- DROP TABLE LOAN_APPLICATION;
-- DROP SEQUENCE SEQ_LOAN_APPLICATION;

CREATE SEQUENCE SEQ_LOAN_APPLICATION START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE LOAN_APPLICATION (
    APPLICATION_NO   NUMBER       PRIMARY KEY,  -- 접수번호 (시퀀스)
    CUSTOMER_NO      VARCHAR2(20) NOT NULL,     -- 고객번호
    ANNUAL_INCOME    NUMBER       NOT NULL,     -- 연소득
    EXISTING_PAYMENT NUMBER       NOT NULL,     -- 기존 대출 연간 원리금
    CREDIT_GRADE     NUMBER       NOT NULL,     -- 신용등급 1~10
    REQUEST_AMOUNT   NUMBER       NOT NULL,     -- 신청금액
    TERM_YEARS       NUMBER       NOT NULL,     -- 대출 기간(년)
    COLLATERAL_VALUE NUMBER       NOT NULL,     -- 담보가치 (0 = 신용대출)
    STATUS           VARCHAR2(20) NOT NULL,     -- IN_PROGRESS / APPROVED / CONDITIONAL / REJECTED
    APPROVED_LIMIT   NUMBER,                    -- 승인 한도
    DSR              NUMBER,                    -- 산출된 DSR
    REJECT_REASON    VARCHAR2(40),              -- 부결 사유
    APPLIED_AT       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 접수 시각
    SCREENED_AT      TIMESTAMP                             -- 심사 시각
);

# 여신 심사 API (학습용 미니 프로젝트)

금융 IT 직무 지원을 준비하며, 은행 여신 심사가 어떤 순서로 진행되는지 찾아보고
그 규칙을 Java / Spring Boot / MyBatis / Oracle 로 직접 구현해 본 프로젝트입니다.

## 만든 이유

앞선 팀 프로젝트(EVRE, allergy-out)에서 "요청을 받아들일 조건과 거절할 조건을 나누어
검증하는" 구조를 다뤄 봤는데, 이 방식이 실제 금융 업무에서는 어떻게 적용되는지
확인해 보고 싶어 여신 심사를 골랐습니다.

## API

| | 메서드 / 경로 | 하는 일 |
|---|---|---|
| 접수 | `POST /api/loans` | 신청 내용을 검증하고 `IN_PROGRESS` 로 저장, 접수번호 반환 |
| 심사 | `POST /api/loans/{applicationNo}/screening` | 접수 건을 읽어 판정하고 결과를 반영 |

## 심사 흐름

```
[접수]  신청 내용 검증  →  중복 접수 검증  →  IN_PROGRESS 저장  →  접수번호 반환
[심사]  접수 조회  →  담보 기준 한도 산출  →  DSR 검증  →  결과 UPDATE
```

### 접수

| 순서 | 검증 | 기준 | 통과하지 못하면 |
|---|---|---|---|
| 1 | 요청 형식 | `@Valid` 필수값, 범위 | **400** `INVALID_INPUT_VALUE` (필드별 메시지) |
| 2 | 신청 내용 | 대출 기간 1~35년, 소득 확인 | **400** `INVALID_TERM` / `NO_INCOME` |
| 3 | 중복 접수 | 같은 고객의 `IN_PROGRESS` 존재 여부 | **409** `DUPLICATE_REQUEST` |

### 심사

| 순서 | 검증 | 기준 | 통과하지 못하면 |
|---|---|---|---|
| 1 | 접수 조회 | 접수번호 존재 여부 | **404** `APPLICATION_NOT_FOUND` |
| 2 | 상태 확인 | `IN_PROGRESS` 인지 | **409** `ALREADY_SCREENED` |
| 3 | 담보 기준 한도 | LTV 70% 상한, 초과 시 한도 축소 | 조건부 승인 |
| 4 | 최소 취급 금액 | 산출 한도 100만원 이상 | **200** `result: REJECTED` |
| 5 | DSR | (기존 연간 원리금 + 신규 연간 원리금) ÷ 연소득 ≤ 40% | **200** `result: REJECTED` |

### 규칙 근거

| 규칙 | 적용 값 | 출처 |
|---|---|---|
| DSR 상한 | 40% | 은행권 가계대출 규제 기준 |
| LTV 상한 | 70% | 주택담보대출 담보인정비율 규제 기준 |
| 심사 기준금리 | 연 5% (고정) | 학습용 단순화 |
| 최소 취급 금액 | 100만원 | 학습용 임의 설정 |

## 접수와 심사를 나눈 이유

처음에는 하나의 요청으로 접수와 심사를 함께 처리했습니다. 그런데 그렇게 하면
`IN_PROGRESS` 상태가 저장될 틈이 없어, 중복 접수 검증이 실제로는 동작하지 않았습니다.

실제 여신 업무는 접수와 심사가 다른 시점에 이뤄지고 그 사이 상태가 `IN_PROGRESS` 입니다.
API 를 둘로 나누자 상태가 실제로 남고, 검증도 동작하게 됐습니다.

한 고객이 여러 건을 동시에 접수하면 각 건은 DSR 을 통과해도 합산하면 초과할 수 있어,
심사 중인 접수가 있으면 새 접수를 받지 않고 한 건씩 처리하도록 했습니다.

## 심사 불가와 부결을 구분한 이유

접수를 진행할 수 없는 경우(400, 404, 409)는 **처리를 시작할 수 없는** 상황이라
예외로 던집니다. 반면 DSR 초과는 **심사를 정상적으로 수행한 결과**이므로
200 응답에 `result: REJECTED` 로 담아 반환합니다.

부결도 이력으로 남겨야 하는데, 예외를 던지면 `@Transactional` 안에서 UPDATE 가
롤백되어 기록이 남지 않는 문제가 있었습니다. 그래서 부결은 결과 값으로 처리했습니다.

사유는 `ApplicationError`(상태 코드 보유) 와 `RejectReason`(부결 사유) 두 enum 으로
나누어 정의해, 새 사유가 필요하면 해당 enum 에만 추가하면 되도록 했습니다.
앞선 프로젝트에서 오류마다 예외 클래스를 늘렸다가 관리가 어려워졌던 경험을
반영한 부분이고, 공통 응답 규격 `ApiResponse<T>` 도 같은 이유로 이어서 사용했습니다.

## 검증 순서를 이렇게 잡은 이유

담보 기준 한도를 먼저 산출한 뒤 그 금액으로 DSR 을 계산합니다.
신청금액 그대로 DSR 을 계산하면 **한도를 낮추면 통과할 수 있는 신청까지 부결**로
처리되기 때문입니다. 순서를 바꾸면 결과가 달라지는 부분입니다.

## 최소 취급 금액을 넣은 이유

처음에는 담보 기준 한도가 0 이면 부결하도록 두었는데, 코드를 다시 보니 신청금액은
`@Min(1)` 로 이미 걸러지고 담보가 0 이면 신용대출로 처리되어, 한도가 0 이 되는 경우는
담보가치가 1 원일 때 뿐이었습니다. 사실상 실행되지 않는 분기였습니다.

실제 은행에는 최소 취급 금액이 있어 그 기준으로 바꿨습니다. 담보 100만원에 100만원을
신청하면 LTV 70% 기준 한도가 70만원이 되어 최소 취급 금액에 미달하므로 부결됩니다.

## 구조

```
com.kh.loan
├── controller
│   └── LoanScreeningController          접수 / 심사 API
├── model
│   ├── dto/LoanApplyRequest             접수 요청 (@Valid 검증)
│   ├── dto/LoanApplyResponse            접수 결과 (접수번호)
│   ├── dto/LoanApplicationDetail         접수 건 조회 결과
│   ├── dto/ScreeningResponse            심사 결과
│   ├── dao/LoanApplicationMapper        접수 저장, 조회, 결과 반영
│   └── service/LoanScreeningService     apply() / screen()
└── global
    ├── common/ApiResponse               code / msg / data 공통 응답
    ├── enums/ApplicationError           진행 불가 사유 + 상태 코드
    ├── enums/RejectReason               심사 부결 사유
    ├── enums/ScreeningResult            APPROVED / CONDITIONAL / REJECTED
    ├── exception/CustomException        진행 불가 예외
    └── exception/GlobalExceptionHandler 응답 형식 통일, 입력 오류는 필드별로 한 번에
```

## 실행

1. SQL Developer 에서 `src/main/resources/schema.sql` 을 실행해 시퀀스와 테이블을 만든다
2. DB 접속 계정을 환경변수로 등록한다
   `application.yml` 의 `username` / `password` 는 값을 직접 적지 않고
   환경변수를 읽도록 해 두었다 (계정 정보를 저장소에 올리지 않기 위해)
3. 애플리케이션 실행
4. postman으로 검증

## 요청 / 응답 예시

### 접수

```
POST /api/loans
{
  "customerNo": "C001",
  "annualIncome": 60000000,
  "existingPayment": 0,
  "creditGrade": 4,
  "requestAmount": 100000000,
  "termYears": 20,
  "collateralValue": 100000000
}
```

```json
{
  "code": 201,
  "msg": "접수되었습니다.",
  "data": { "applicationNo": 1, "status": "IN_PROGRESS" }
}
```

### 심사

```
POST /api/loans/1/screening
```

```json
{
  "code": 200,
  "msg": "심사가 완료되었습니다.",
  "data": {
    "applicationNo": 1,
    "result": "CONDITIONAL",
    "approvedLimit": 70000000,
    "dsr": 0.0936,
    "note": "담보가치 대비 한도(LTV 70%) 기준으로 한도가 조정되었습니다."
  }
}
```

담보 1억에 1억을 신청했으므로 LTV 70% 기준으로 한도가 7,000만원으로 조정되고,
그 금액으로 계산한 DSR 9.36% 가 상한 이내여서 조건부 승인으로 처리됩니다.

### 오류

```json
{
  "code": 400,
  "msg": "입력값이 올바르지 않습니다.",
  "data": {
    "customerNo": "고객번호는 필수입니다.",
    "annualIncome": "연소득은 필수입니다."
  }
}
```

## 검증

Postman 으로 호출하고 `LOAN_APPLICATION` 테이블에 저장된 결과를 확인했습니다.

### 심사 결과

| 케이스 | 담보 | 신청 | 기간 | 연소득 / 기존상환 | 결과 | 승인 한도 | DSR |
|---|---|---|---|---|---|---|---|
| 담보 여유 | 2억 | 1억 | 20년 | 6,000만 / 0 | `APPROVED` | 1억 | 13.37% |
| LTV 초과 | 1억 | 1억 | 20년 | 6,000만 / 0 | `CONDITIONAL` | 7,000만 | 9.36% |
| 신용대출 | 0 | 3,000만 | 10년 | 6,000만 / 0 | `APPROVED` | 3,000만 | 6.48% |
| 최소금액 미달 | 100만 | 100만 | 5년 | 6,000만 / 0 | `REJECTED` | 0 | - |
| DSR 초과 | 4억 | 2억 | 20년 | 3,000만 / 1,000만 | `REJECTED` | 0 | 86.83% |

### 진행 불가

| 케이스 | 요청 | 응답 |
|---|---|---|
| 필수값 누락 | 접수 | **400** `INVALID_INPUT_VALUE` + 필드별 메시지 |
| 기간 40년 | 접수 | **400** `INVALID_TERM` |
| 연소득 0 | 접수 | **400** `NO_INCOME` |
| 심사 안 받고 같은 고객 재접수 | 접수 | **409** `DUPLICATE_REQUEST` |
| 없는 접수번호 | 심사 | **404** `APPLICATION_NOT_FOUND` |
| 이미 심사한 접수 | 심사 | **409** `ALREADY_SCREENED` |
| 접수번호에 문자 | 심사 | **400** `INVALID_INPUT_VALUE` |

부결 케이스도 `STATUS = 'REJECTED'`, `REJECT_REASON` 과 함께 저장되는 것을
확인했습니다. 400, 404, 409 로 걸러진 요청은 처리 전에 중단되므로 이력이 남지 않습니다.

## 개발 환경

| 항목 | 값 |
|---|---|
| Spring Boot | 4.1.1 |
| Java | 21 |
| Build | Gradle (Groovy) |
| DB | Oracle (로컬) |
| Dependencies | Spring Web, Validation, JDBC, MyBatis, Lombok, ojdbc17 |

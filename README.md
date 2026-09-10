# 여신 심사 API (학습용 미니 프로젝트)

금융 IT 직무 지원을 준비하며, 은행 여신 심사가 어떤 순서로 진행되는지 찾아보고
그 규칙을 Java / Spring Boot / MyBatis 로 직접 구현해 본 프로젝트입니다.

## 만든 이유

앞선 팀 프로젝트(EVRE, allergy-out)에서 "요청을 받아들일 조건과 거절할 조건을 나누어
검증하는" 구조를 다뤄 봤는데, 이 방식이 실제 금융 업무에서는 어떻게 적용되는지
확인해 보고 싶어 여신 심사를 골랐습니다.

## 심사 흐름

```
신청 접수 → 신청 내용 검증 → 중복 신청 검증 → 담보 기준 한도 산출 → DSR 검증 → 결과 저장
```

| 순서 | 검증 | 기준 | 통과하지 못하면 |
|---|---|---|---|
| 1 | 신청 내용 | 대출 기간 1~35년, 소득 확인 | **400** `INVALID_TERM` / `NO_INCOME` |
| 2 | 중복 신청 | 같은 고객의 진행 중 신청 존재 여부 | **409** `DUPLICATE_REQUEST` |
| 3 | 담보 기준 한도 | LTV 70% 상한, 초과 시 한도 축소 | 조건부 승인 |
| 3-1 | 최소 취급 금액 | 산출 한도 100만원 이상 | **200** `result: REJECTED` |
| 4 | DSR | (기존 연간 원리금 + 신규 연간 원리금) ÷ 연소득 ≤ 40% | **200** `result: REJECTED` |

### 규칙 근거

| 규칙 | 적용 값 | 출처 |
|---|---|---|
| DSR 상한 | 40% | 은행권 가계대출 규제 기준 |
| LTV 상한 | 70% | 주택담보대출 담보인정비율 규제 기준 |
| 심사 기준금리 | 연 5% (고정) | 학습용 단순화 |
| 최소 취급 금액 | 100만원 | 학습용 임의 설정 |

### 검증 순서를 이렇게 잡은 이유

담보 기준 한도(3단계)를 먼저 산출한 뒤 그 금액으로 DSR(4단계)을 계산합니다.
신청금액 그대로 DSR을 계산하면 **한도를 낮추면 통과할 수 있는 신청까지 부결**로
처리되기 때문입니다. 순서를 바꾸면 결과가 달라지는 부분이라 테스트로 고정했습니다.

## 심사 불가와 부결을 구분한 이유

요청 자체가 잘못됐거나 기존 신청과 충돌하는 경우(400, 409)는 **심사를 시작할 수 없는**
상황이라 예외로 처리했습니다. 반면 DSR 초과는 **심사를 정상적으로 수행한 결과**이므로
200 응답에 `result: REJECTED` 로 담아 반환합니다.

부결도 이력으로 남겨야 하는데, 예외를 던지면 `@Transactional` 안에서 저장이 롤백되어
기록이 남지 않는 문제가 있었습니다. 그래서 부결은 결과 값으로 처리하도록 바꿨습니다.

사유는 `ApplicationError` 와 `RejectReason` 두 enum 으로 나누어 정의해, 새 사유가
필요하면 해당 enum 에만 추가하면 되도록 했습니다. 앞선 프로젝트에서 오류마다 예외
클래스를 늘렸다가 관리가 어려워졌던 경험을 반영한 부분입니다.

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
│   └── LoanScreeningController          POST /api/loans/screening
├── model
│   ├── dto/ScreeningRequest             신청 정보 (@Valid 검증)
│   ├── dto/ScreeningResponse            심사 결과
│   ├── dao/LoanApplicationMapper        진행 중 신청 조회, 심사 결과 저장
│   └── service/LoanScreeningService     검증 4단계를 순서대로 호출
└── global
    ├── enums/ApplicationError           심사 불가 사유 + 상태 코드 (400 / 409)
    ├── enums/RejectReason               심사 부결 사유
    ├── enums/ScreeningResult            APPROVED / CONDITIONAL / REJECTED
    ├── exception/ScreeningException     심사 불가 예외
    └── exception/GlobalExceptionHandler 상태 코드 응답, 입력 오류는 필드별로 한 번에
```

## 실행

1. SQL Developer 에서 `src/main/resources/schema.sql` 을 한 번 실행해 테이블을 만든다
2. 애플리케이션 실행

```
gradlew.bat bootRun    # http://localhost:8080
```

DB 는 로컬 Oracle 을 사용한다. 접속 정보는 `application.yml` 에 있고,
`spring.sql.init.mode` 는 `never` 로 두어 실행할 때마다 DDL 이 돌지 않게 했다.
매퍼에서 실행되는 SQL 은 콘솔 로그로 확인할 수 있다.

## 요청 / 응답 예시

```
POST /api/loans/screening
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
  "result": "CONDITIONAL",
  "approvedLimit": 70000000,
  "dsr": 0.0936,
  "note": "담보가치 대비 한도(LTV 70%) 기준으로 한도가 조정되었습니다."
}
```

담보 1억에 1억을 신청했으므로 LTV 70% 기준으로 한도가 7,000만원으로 조정되고,
그 금액으로 계산한 DSR 9.36% 가 상한 이내여서 조건부 승인으로 처리됩니다.

## 검증

Postman 으로 호출하고 DB 에 저장된 결과를 확인했습니다.

| 케이스 | 담보 | 신청 | 기간 | 연소득 / 기존상환 | 결과 | 승인 한도 | DSR |
|---|---|---|---|---|---|---|---|
| 담보 여유 | 2억 | 1억 | 20년 | 6,000만 / 0 | `APPROVED` | 1억 | 13.37% |
| LTV 초과 | 1억 | 1억 | 20년 | 6,000만 / 0 | `CONDITIONAL` | 7,000만 | 9.36% |
| 신용대출 | 0 | 3,000만 | 10년 | 6,000만 / 0 | `APPROVED` | 3,000만 | 6.48% |
| 최소금액 미달 | 100만 | 100만 | 5년 | 6,000만 / 0 | `REJECTED` | 0 | - |
| DSR 초과 | 4억 | 2억 | 20년 | 3,000만 / 1,000만 | `REJECTED` | 0 | 86.83% |
| 기간 초과 | 2억 | 1억 | 40년 | 6,000만 / 0 | **400** `INVALID_TERM` | - | - |
| 소득 미확인 | 1억 | 5,000만 | 10년 | **0** / 0 | **400** `NO_INCOME` | - | - |
| 필수값 누락 | - | - | - | - | **400** `INVALID_INPUT` | - | - |
| 중복 접수 | 0 | 5,000만 | 10년 | 6,000만 / 0 | **409** `DUPLICATE_REQUEST` | - | - |

중복 접수는 `STATUS = 'IN_PROGRESS'` 인 행을 직접 넣어 두고 같은 고객번호로 호출해
확인했습니다.

부결 케이스도 `LOAN_APPLICATION` 테이블에 `STATUS = 'REJECTED'`,
`REJECT_REASON` 과 함께 저장되는 것을 확인했습니다. 400, 409 로 걸러진 요청은
심사 전에 중단되므로 이력이 남지 않습니다.

## 개발 환경

| 항목 | 값 |
|---|---|
| Spring Boot | 4.1.1 |
| Java | 21 |
| Build | Gradle (Groovy) |
| DB | Oracle (로컬) |
| Dependencies | Spring Web, Validation, JDBC, MyBatis, Lombok, ojdbc17 |

## 한계

- 접수와 심사를 한 번의 요청으로 처리해, 실제로는 `IN_PROGRESS` 상태가 저장되지 않습니다.
  중복 접수 검증은 구현했지만 접수 단계를 별도 API 로 분리해야 실제로 동작합니다.
- 테스트 코드 없이 Postman 호출과 DB 조회로 검증했습니다.
- 금액 계산에 `double` 을 사용했습니다. 부동소수점 오차가 누적될 수 있어
  실무에서는 `BigDecimal` 을 써야 하는 부분입니다.
- 신용등급을 입력만 받고 금리 산출에는 반영하지 않았습니다. (등급별 가산금리 테이블 미구현)
- 개인 여신만 다루었고 기업 여신의 재무분석·평가모형은 다루지 않았습니다.
- 상환 방식은 원리금균등 한 가지만 가정했습니다.

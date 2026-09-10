package com.kh.loan.global.enums;

import org.springframework.http.HttpStatus;

import lombok.Getter;

// 접수 또는 심사를 진행할 수 없는 경우
// 요청 자체에 문제가 있거나 기존 데이터와 충돌하는 상황이므로 예외로 던진다
// 심사를 정상 수행한 결과인 부결 사유는 RejectReason 에 따로 정의한다
@Getter
public enum ApplicationError {

	// 묶음 코드 - 구체 문구는 응답 data({필드: 메시지})로 전달
	INVALID_INPUT_VALUE  (HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

	// 개별 코드 - msg 자체가 구체 문구, data 없음
	INVALID_TERM         (HttpStatus.BAD_REQUEST, "대출 기간은 1년 이상 35년 이하만 신청할 수 있습니다."),
	NO_INCOME            (HttpStatus.BAD_REQUEST, "소득이 확인되지 않아 접수할 수 없습니다."),
	DUPLICATE_REQUEST    (HttpStatus.CONFLICT,    "이미 심사가 진행 중인 접수가 있습니다."),
	APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND,   "해당 접수번호를 찾을 수 없습니다."),
	ALREADY_SCREENED     (HttpStatus.CONFLICT,    "이미 심사가 완료된 접수입니다."),

	// 요청 잘못이 아닌 경우
	SAVE_FAILED          (HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다."),
	DB_ERROR             (HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;

	ApplicationError(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}
}

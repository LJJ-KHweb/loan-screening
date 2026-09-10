package com.kh.loan.global.enums;

// 심사는 정상적으로 진행되었지만 결과가 부결인 경우의 사유
// 새 사유가 필요하면 이곳에만 추가한다
public enum RejectReason {

	DSR_EXCEEDED    ("총부채원리금상환비율(DSR)이 한도를 초과했습니다."),
	BELOW_MIN_AMOUNT("산출된 한도가 최소 취급 금액에 미달합니다.");

	private final String message;

	RejectReason(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}

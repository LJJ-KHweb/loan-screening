package com.kh.loan.model.dto;

import com.kh.loan.global.enums.RejectReason;
import com.kh.loan.global.enums.ScreeningResult;

import lombok.Getter;

@Getter
public class ScreeningResponse {

	private final long applicationNo;

	private final ScreeningResult result;

	// 승인 한도 (부결이면 0)
	private final long approvedLimit;

	// 산출된 DSR
	private final double dsr;

	// 조건부 승인 사유 또는 부결 사유
	private final String note;

	private ScreeningResponse(long applicationNo, ScreeningResult result,
	                          long approvedLimit, double dsr, String note) {
		this.applicationNo = applicationNo;
		this.result = result;
		this.approvedLimit = approvedLimit;
		this.dsr = dsr;
		this.note = note;
	}

	public static ScreeningResponse approved(long applicationNo, long limit, double dsr) {
		return new ScreeningResponse(applicationNo, ScreeningResult.APPROVED, limit, dsr, null);
	}

	public static ScreeningResponse conditional(long applicationNo, long limit, double dsr, String note) {
		return new ScreeningResponse(applicationNo, ScreeningResult.CONDITIONAL, limit, dsr, note);
	}

	public static ScreeningResponse rejected(long applicationNo, double dsr, RejectReason reason) {
		return new ScreeningResponse(applicationNo, ScreeningResult.REJECTED, 0L, dsr, reason.getMessage());
	}
}

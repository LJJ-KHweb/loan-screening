package com.kh.loan.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 접수 결과. 이 접수번호로 심사를 요청한다
@Getter
@AllArgsConstructor
public class LoanApplyResponse {

	private long applicationNo;
	private String status;
}

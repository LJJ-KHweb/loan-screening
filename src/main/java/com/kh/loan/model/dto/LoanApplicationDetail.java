package com.kh.loan.model.dto;

import lombok.Getter;
import lombok.Setter;

// LOAN_APPLICATION 테이블 조회 결과
// 메인 클래스(LoanApplication)와 이름이 겹치지 않도록 Detail 을 붙였다
@Getter
@Setter
public class LoanApplicationDetail {

	private long applicationNo;
	private String customerNo;
	private long annualIncome;
	private long existingPayment;
	private int creditGrade;
	private long requestAmount;
	private int termYears;
	private long collateralValue;
	private String status;
	private Long approvedLimit;
	private Double dsr;
	private String rejectReason;
}

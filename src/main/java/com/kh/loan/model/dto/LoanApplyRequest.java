package com.kh.loan.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 여신 접수 요청
// applicationNo 는 요청으로 받지 않고, INSERT 직전에 시퀀스 값이 채워진다
@Getter
@Setter
public class LoanApplyRequest {

	private Long applicationNo;

	@NotBlank(message = "고객번호는 필수입니다.")
	private String customerNo;

	@NotNull(message = "연소득은 필수입니다.")
	@Min(value = 0, message = "연소득은 0 이상이어야 합니다.")
	private Long annualIncome;

	@NotNull(message = "기존 연간 원리금은 필수입니다.")
	@Min(value = 0, message = "기존 연간 원리금은 0 이상이어야 합니다.")
	private Long existingPayment;

	@NotNull(message = "신용등급은 필수입니다.")
	@Min(value = 1, message = "신용등급은 1 이상이어야 합니다.")
	private Integer creditGrade;

	@NotNull(message = "신청금액은 필수입니다.")
	@Min(value = 1, message = "신청금액은 1 이상이어야 합니다.")
	private Long requestAmount;

	@NotNull(message = "대출 기간은 필수입니다.")
	private Integer termYears;

	@NotNull(message = "담보가치는 필수입니다. 신용대출이면 0을 입력합니다.")
	@Min(value = 0, message = "담보가치는 0 이상이어야 합니다.")
	private Long collateralValue;
}

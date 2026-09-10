package com.kh.loan.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.loan.global.common.ApiResponse;
import com.kh.loan.model.dto.LoanApplyRequest;
import com.kh.loan.model.dto.LoanApplyResponse;
import com.kh.loan.model.dto.ScreeningResponse;
import com.kh.loan.model.service.LoanScreeningService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanScreeningController {

	private final LoanScreeningService loanScreeningService;

	// 여신 접수. 접수번호를 돌려준다
	@PostMapping
	public ResponseEntity<ApiResponse<LoanApplyResponse>> apply(@Valid @RequestBody LoanApplyRequest req) {
		LoanApplyResponse result = loanScreeningService.apply(req);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("접수되었습니다.", result));
	}

	// 접수번호로 심사 요청. 승인, 조건부승인, 부결 모두 200 으로 결과를 반환한다
	@PostMapping("/{applicationNo}/screening")
	public ResponseEntity<ApiResponse<ScreeningResponse>> screen(@PathVariable("applicationNo") long applicationNo) {
		ScreeningResponse result = loanScreeningService.screen(applicationNo);
		return ResponseEntity.ok(ApiResponse.success("심사가 완료되었습니다.", result));
	}
}

package com.kh.loan.global.exception;

import java.util.Map;

import com.kh.loan.global.enums.ApplicationError;

import lombok.Getter;

// 서비스에서 직접 던지는 예외
// 상태 코드와 메시지는 ApplicationError 가 들고 있고, 필드별 상세는 details 에 담는다
@Getter
public class CustomException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final ApplicationError error;

	// {필드명: 구체 메시지}, 없으면 null
	private final Map<String, String> details;

	public CustomException(ApplicationError error) {
		this(error, null);
	}

	public CustomException(ApplicationError error, Map<String, String> details) {
		super(error.getMessage());
		this.error = error;
		this.details = details;
	}
}

package com.kh.loan.global.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.kh.loan.global.common.ApiResponse;
import com.kh.loan.global.enums.ApplicationError;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 서비스 코드에서 직접 던지는 예외 (CustomException(ApplicationError.XXX))
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleCustomException(CustomException e) {
		ApplicationError error = e.getError();
		log.warn("CustomException: {}", e.getMessage());
		return ResponseEntity.status(error.getStatus())
				.body(ApiResponse.fail(error.getStatus().value(),
									   error.getMessage(),
									   e.getDetails()));
	}

	// @Valid 검증 실패 (요청 DTO의 @NotNull 등)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
		Map<String, String> details = new LinkedHashMap<>();
		for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
			details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		log.warn("Validation failed: {}", details);
		return ResponseEntity.status(ApplicationError.INVALID_INPUT_VALUE.getStatus())
				.body(ApiResponse.fail(ApplicationError.INVALID_INPUT_VALUE.getStatus().value(),
									   ApplicationError.INVALID_INPUT_VALUE.getMessage(),
									   details));
	}

	// 경로 파라미터 타입 불일치 (예: POST /api/loans/abc/screening) -> 400
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		log.warn("Type mismatch: {} = {}", e.getName(), e.getValue());
		return ResponseEntity.status(ApplicationError.INVALID_INPUT_VALUE.getStatus())
				.body(ApiResponse.fail(ApplicationError.INVALID_INPUT_VALUE.getStatus().value(),
									   ApplicationError.INVALID_INPUT_VALUE.getMessage()));
	}

	// DB 오류 - 연결 실패, 문법 오류, 제약조건 위반 등
	// 원인은 서버 로그에만 남기고 응답에는 담지 않는다 (테이블명, SQL 노출 방지)
	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException e) {
		log.error("DB 처리 중 오류", e);
		return ResponseEntity.status(ApplicationError.DB_ERROR.getStatus())
				.body(ApiResponse.fail(ApplicationError.DB_ERROR.getStatus().value(),
									   ApplicationError.DB_ERROR.getMessage()));
	}

	// 예상 못한 나머지 전부
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(ApplicationError.INTERNAL_SERVER_ERROR.getStatus())
				.body(ApiResponse.fail(ApplicationError.INTERNAL_SERVER_ERROR.getStatus().value(),
									   ApplicationError.INTERNAL_SERVER_ERROR.getMessage()));
	}
}

package com.kh.loan.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 모든 응답을 code / msg / data 한 가지 형태로 통일한다
// 화면이 응답 구조를 하나만 알면 되도록 앞선 프로젝트에서 쓴 규격을 그대로 가져왔다
@Getter
@AllArgsConstructor
public class ApiResponse<T> {

	private int code;
	private String msg;
	private T data;

	public static <T> ApiResponse<T> success(String msg, T data) {
		return new ApiResponse<>(200, msg, data);
	}

	public static <T> ApiResponse<T> created(String msg, T data) {
		return new ApiResponse<>(201, msg, data);
	}

	public static <T> ApiResponse<T> fail(int code, String msg) {
		return new ApiResponse<>(code, msg, null);
	}

	public static <T> ApiResponse<T> fail(int code, String msg, T data) {
		return new ApiResponse<>(code, msg, data);
	}
}

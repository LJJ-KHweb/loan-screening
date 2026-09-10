package com.kh.loan.model.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kh.loan.model.dto.LoanApplicationDetail;
import com.kh.loan.model.dto.LoanApplyRequest;

@Mapper
public interface LoanApplicationMapper {

	// 같은 고객의 심사 진행 중인 접수 건수
	int countInProgress(@Param("customerNo") String customerNo);

	// 접수 저장. 시퀀스에서 받은 번호가 req.applicationNo 에 채워진다
	int insertApplication(LoanApplyRequest req);

	// 접수번호로 한 건 조회
	LoanApplicationDetail findByNo(@Param("applicationNo") long applicationNo);

	// 심사 결과 반영
	int updateResult(@Param("applicationNo") long applicationNo,
	                 @Param("status") String status,
	                 @Param("approvedLimit") Long approvedLimit,
	                 @Param("dsr") Double dsr,
	                 @Param("rejectReason") String rejectReason);
}

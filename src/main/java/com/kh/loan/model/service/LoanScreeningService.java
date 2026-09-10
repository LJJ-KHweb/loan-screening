package com.kh.loan.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.loan.global.enums.ApplicationError;
import com.kh.loan.global.enums.RejectReason;
import com.kh.loan.global.enums.ScreeningResult;
import com.kh.loan.global.exception.CustomException;
import com.kh.loan.model.dao.LoanApplicationMapper;
import com.kh.loan.model.dto.LoanApplicationDetail;
import com.kh.loan.model.dto.LoanApplyRequest;
import com.kh.loan.model.dto.LoanApplyResponse;
import com.kh.loan.model.dto.ScreeningResponse;

import lombok.RequiredArgsConstructor;

/* 여신 접수와 심사
 *
 * [접수] apply()
 *   1. 신청 내용 검증 : 기간 1~35년, 소득 확인           -> 400
 *   2. 중복 접수 검증 : 심사 진행 중인 접수가 있으면 거절   -> 409
 *   3. IN_PROGRESS 상태로 저장하고 접수번호를 돌려준다
 *
 * [심사] screen()
 *   1. 접수 조회      : 없으면 404, 이미 심사됐으면 409
 *   2. 담보 기준 한도 : LTV 70% 상한, 초과 시 한도 축소
 *                       최소 취급 금액 미달이면 부결
 *   3. DSR 검증      : 산출 한도 기준 40% 초과면 부결
 *   4. 결과를 UPDATE 한다
 *
 * 접수를 심사와 분리한 이유
 * 실제 여신 업무는 접수와 심사가 다른 시점에 이뤄지고, 그 사이 상태가 IN_PROGRESS 다
 * 한 번의 요청으로 처리하면 이 상태가 저장되지 않아 중복 접수 검증이 동작하지 않는다
 *
 * 부결을 예외로 던지지 않는 이유
 * 접수를 진행할 수 없는 경우(400, 404, 409)는 예외로 던지지만
 * 부결은 심사를 정상적으로 수행한 결과다
 * 예외로 던지면 @Transactional 안에서 UPDATE 가 롤백되어 부결 이력이 남지 않는다
 */

@Service
@RequiredArgsConstructor
public class LoanScreeningService {

	// 총부채원리금상환비율 상한 - 은행권 가계대출 규제 기준
	private static final double DSR_LIMIT = 0.40;

	// 담보인정비율 상한 - 주택담보대출 규제 기준
	private static final double LTV_LIMIT = 0.70;

	// 심사에 사용하는 기준 금리 (연, 고정)
	private static final double SCREENING_RATE = 0.05;

	// 최소 취급 금액 - 산출 한도가 이보다 적으면 취급하지 않는다
	private static final long MIN_LOAN_AMOUNT = 1_000_000L;

	private static final int MIN_TERM_YEARS = 1;
	private static final int MAX_TERM_YEARS = 35;

	private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

	private final LoanApplicationMapper loanApplicationMapper;

	// -------------------- 접수 --------------------

	@Transactional
	public LoanApplyResponse apply(LoanApplyRequest req) {

		validateApplication(req);
		validateNotDuplicated(req.getCustomerNo());

		// DB에 대출 접수를 Insert
		int saved = loanApplicationMapper.insertApplication(req);

		// 저장된 행이 없으면 접수번호를 돌려줄 수 없으므로 여기서 중단한다
		if (saved < 1) {
			throw new CustomException(ApplicationError.SAVE_FAILED);
		}

		return new LoanApplyResponse(req.getApplicationNo(), STATUS_IN_PROGRESS);
	}

	// 신청 내용 자체가 접수 대상인지 확인
	private void validateApplication(LoanApplyRequest req) {
		int term = req.getTermYears();
		// 사용자가 선택한 대출 기간이 내가 정한 대출 기간내에 있는지 조건
		if (term < MIN_TERM_YEARS || term > MAX_TERM_YEARS) {
			throw new CustomException(ApplicationError.INVALID_TERM);
		}
		// DSR 계산에서 연소득을 분모로 사용하므로 0 이면 접수 단계에서 거른다
		if (req.getAnnualIncome() <= 0) {
			throw new CustomException(ApplicationError.NO_INCOME);
		}
	}

	// 이미 접수했는데 심사 끝나지 않은 고객인지 확인
	private void validateNotDuplicated(String customerNo) {
		if (loanApplicationMapper.countInProgress(customerNo) > 0) {
			throw new CustomException(ApplicationError.DUPLICATE_REQUEST);
		}
	}

	// -------------------- 심사 --------------------

	@Transactional
	public ScreeningResponse screen(long applicationNo) {

		LoanApplicationDetail app = findScreenable(applicationNo);

		long limit = calculateLimitByCollateral(app);

		// 담보 기준으로 산출된 한도가 최소 취급 금액에 못 미치면 부결
		if (limit < MIN_LOAN_AMOUNT) {
			return reject(app, 0, RejectReason.BELOW_MIN_AMOUNT);
		}
		double dsr = calculateDsr(app, limit);

		//은행권 가계대출 규제 기준을 넘으면 부결
		if (dsr > DSR_LIMIT) {
			return reject(app, dsr, RejectReason.DSR_EXCEEDED);
		}

		// 대출금액보다 최대 대출금액이 낮으면 대출금액이 최대 대출금액으로 조정된다.
		if (limit < app.getRequestAmount()) {
			update(app, ScreeningResult.CONDITIONAL, limit, dsr, null);
			return ScreeningResponse.conditional(applicationNo, limit, round(dsr),
					"담보가치 대비 한도(LTV 70%) 기준으로 한도가 조정되었습니다.");
		}

		// 사용자가 원한 대출금액 승인
		update(app, ScreeningResult.APPROVED, limit, dsr, null);
		return ScreeningResponse.approved(applicationNo, limit, round(dsr));
	}

	// 심사할 수 있는 접수인지 확인
	private LoanApplicationDetail findScreenable(long applicationNo) {
		LoanApplicationDetail app = loanApplicationMapper.findByNo(applicationNo);

		// 접수 결과가 있는지
		if (app == null) {
			throw new CustomException(ApplicationError.APPLICATION_NOT_FOUND);
		}
		// 접수 결과가 있는데 STATUS가 IN_PROGRESS가 맞는지 확인
		if (!STATUS_IN_PROGRESS.equals(app.getStatus())) {
			throw new CustomException(ApplicationError.ALREADY_SCREENED);
		}
		return app;
	}

	// 담보가 있으면 LTV 상한까지만 인정한다
	// 담보가 없으면 신용대출로 보고 신청금액을 그대로 둔다
	private long calculateLimitByCollateral(LoanApplicationDetail app) {
		long collateral = app.getCollateralValue();
		long request = app.getRequestAmount();

		if (collateral == 0) {
			return request;
		}
		long ltvLimit = (long) (collateral * LTV_LIMIT);
		return Math.min(request, ltvLimit);
	}

	// DSR = (기존 연간 원리금 + 신규 연간 원리금) / 연소득
	private double calculateDsr(LoanApplicationDetail app, long limit) {
		double newYearlyPayment = yearlyPayment(limit, SCREENING_RATE, app.getTermYears());
		return (app.getExistingPayment() + newYearlyPayment) / (double) app.getAnnualIncome();
	}

	// 원리금균등 상환 기준 연간 상환액
	private double yearlyPayment(long principal, double rate, int years) {
		return principal * rate / (1 - Math.pow(1 + rate, -years));
	}

	private ScreeningResponse reject(LoanApplicationDetail app, double dsr, RejectReason reason) {
		update(app, ScreeningResult.REJECTED, 0L, dsr, reason);
		return ScreeningResponse.rejected(app.getApplicationNo(), round(dsr), reason);
	}

	private void update(LoanApplicationDetail app, ScreeningResult result,
	                    Long limit, double dsr, RejectReason reason) {
		int updated = loanApplicationMapper.updateResult(app.getApplicationNo(), result.name(), limit,
				round(dsr), reason == null ? null : reason.name());

		// 심사 결과가 반영되지 않았다면 응답만 성공으로 나가면 안 된다
		if (updated < 1) {
			throw new CustomException(ApplicationError.SAVE_FAILED);
		}
	}

	//소숫점 4자리만 
	private double round(double value) {
		return Math.round(value * 10000) / 10000.0;
	}
}

package com.abcbank.atm.impl;

import com.abcbank.atm.interfaces.LoanCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * StandardLoanCalculator — Requirement 2: Interface 实现类
 *
 * 标准贷款计算器，使用等额本息公式:
 *   Monthly = P × r × (1+r)^n / ((1+r)^n - 1)
 *   其中 r = annualRate / 12, n = months
 *
 * 对应 PDF 中的 Loan Repayment Calculator 模块。
 */
public class StandardLoanCalculator implements LoanCalculator {

    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");
    private static final int SCALE = 10;

    @Override
    public BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal == null || annualRate == null || months <= 0) {
            return BigDecimal.ZERO;
        }

        // 月利率 r = annualRate / 12
        BigDecimal monthlyRate = annualRate.divide(MONTHS_PER_YEAR, SCALE, RoundingMode.HALF_UP);

        // 如果利率为 0，直接平均分摊
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);
        }

        // (1 + r)^n
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(months);

        // P × r × (1+r)^n / ((1+r)^n - 1)
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateTotalRepayment(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthly = calculateMonthlyPayment(principal, annualRate, months);
        return monthly.multiply(new BigDecimal(months)).setScale(2, RoundingMode.HALF_UP);
    }
}

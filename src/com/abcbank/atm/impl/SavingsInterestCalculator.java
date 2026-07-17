package com.abcbank.atm.impl;

import com.abcbank.atm.interfaces.InterestCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SavingsInterestCalculator — Requirement 2: Interface 实现类
 *
 * 储蓄账户利息计算器，使用简单利息公式:
 *   Interest = Principal × Rate × (Months / 12)
 *
 * 对应 PDF 中的 Savings Account Interest Calculator 模块。
 */
public class SavingsInterestCalculator implements InterestCalculator {

    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    @Override
    public BigDecimal calculateInterest(BigDecimal principal, BigDecimal rate, int months) {
        if (principal == null || rate == null) {
            return BigDecimal.ZERO;
        }
        if (months <= 0) {
            return BigDecimal.ZERO;
        }

        // Simple Interest = Principal × Rate × (Months / 12)
        BigDecimal monthFactor = new BigDecimal(months).divide(MONTHS_PER_YEAR, 10, RoundingMode.HALF_UP);
        return principal.multiply(rate).multiply(monthFactor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 获取利息计算说明，用于用户界面展示
     */
    public String getCalculationDetail(BigDecimal principal, BigDecimal rate, int months) {
        BigDecimal interest = calculateInterest(principal, rate, months);
        return String.format("本金 $%s × 年利率 %.2f%% × (%d/12) 月 = 预估利息 $%s",
                principal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                rate.multiply(new BigDecimal("100")).doubleValue(),
                months,
                interest.toPlainString());
    }
}

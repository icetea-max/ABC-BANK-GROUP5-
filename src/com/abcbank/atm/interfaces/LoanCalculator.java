package com.abcbank.atm.interfaces;

import java.math.BigDecimal;

/**
 * LoanCalculator 接口 — Requirement 2: Interface 用于 Class 设计
 *
 * PDF 规定接口之一，用于计算贷款还款（对应 Loan Repayment Calculator 模块）。
 * 所有贷款计算类必须实现此接口，确保审计合规。
 */
public interface LoanCalculator {

    /**
     * 计算每月还款金额（等额本息）
     *
     * @param principal  贷款本金
     * @param annualRate 年利率（小数形式，如 0.05 表示 5%）
     * @param months     还款期数（月）
     * @return 每月固定还款金额
     */
    BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months);

    /**
     * 计算总还款金额（本金 + 利息）
     *
     * @param principal  贷款本金
     * @param annualRate 年利率
     * @param months     还款期数（月）
     * @return 总还款金额
     */
    BigDecimal calculateTotalRepayment(BigDecimal principal, BigDecimal annualRate, int months);
}

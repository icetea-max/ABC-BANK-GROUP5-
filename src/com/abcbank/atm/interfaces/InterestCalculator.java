package com.abcbank.atm.interfaces;

import java.math.BigDecimal;

/**
 * InterestCalculator 接口 — Requirement 2: Interface 用于 Class 设计
 *
 * PDF 规定接口之一，用于计算利息（对应 Savings Account Interest Calculator 模块）。
 * 所有利息计算类必须实现此接口，确保审计合规。
 */
public interface InterestCalculator {

    /**
     * 根据本金、年利率和期限计算利息
     *
     * @param principal 本金
     * @param rate      年利率（小数形式，如 0.035 表示 3.5%）
     * @param months    期限（月数）
     * @return 计算出的利息金额
     */
    BigDecimal calculateInterest(BigDecimal principal, BigDecimal rate, int months);
}

package com.abcbank.atm.interfaces;

import java.math.BigDecimal;

/**
 * PaymentPolicy 接口 — Requirement 2: Interface 用于 Class 设计
 *
 * PDF 规定接口之一，用于定义支付/交易审批策略
 * （对应 Credit Card Payment System 和 ATM Withdrawal System 模块）。
 * 所有支付审批策略必须实现此接口，确保审计合规。
 */
public interface PaymentPolicy {

    /**
     * 审批交易 — 根据账户余额和交易金额判断是否批准
     *
     * @param balance 当前账户余额
     * @param amount  请求交易金额
     * @return true 批准交易, false 拒绝交易
     */
    boolean approveTransaction(BigDecimal balance, BigDecimal amount);

    /**
     * 返回策略描述，用于审计日志
     *
     * @return 策略名称和规则说明
     */
    String getPolicyDescription();
}

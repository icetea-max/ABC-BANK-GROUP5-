package com.abcbank.atm.impl;

import com.abcbank.atm.interfaces.PaymentPolicy;

import java.math.BigDecimal;

/**
 * StandardPaymentPolicy — Requirement 2: Interface 实现类
 *
 * 标准支付策略: 余额 >= 交易金额即批准（标准透支保护）。
 * 银行审计合规的标准实现。
 */
public class StandardPaymentPolicy implements PaymentPolicy {

    private static final String POLICY_NAME = "Standard Overdraft Protection Policy";

    @Override
    public boolean approveTransaction(BigDecimal balance, BigDecimal amount) {
        // 标准规则: 余额足够即可批准
        return balance.compareTo(amount) >= 0;
    }

    @Override
    public String getPolicyDescription() {
        return POLICY_NAME + " — 规则: 账户余额 >= 交易金额即可批准交易";
    }

    /**
     * 获取拒绝原因，用于向用户解释为何交易被拒绝
     */
    public String getRejectionReason(BigDecimal balance, BigDecimal amount) {
        return String.format("根据 %s: 余额不足 — 当前余额 $%s, 请求金额 $%s",
                POLICY_NAME,
                balance.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
    }
}

package com.abcbank.atm.exception;

import java.math.BigDecimal;

/**
 * InsufficientBalanceException — Requirement 4: Exception Handling
 *
 * 当账户余额不足以完成交易时抛出。
 * 包含当前余额和请求金额的具体信息。
 */
public class InsufficientBalanceException extends Exception {

    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("[拒绝] 余额不足! 当前余额: $%s, 交易金额: $%s",
                format(currentBalance), format(requestedAmount)));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public InsufficientBalanceException(String message) {
        super(message);
        this.currentBalance = BigDecimal.ZERO;
        this.requestedAmount = BigDecimal.ZERO;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    private static String format(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}

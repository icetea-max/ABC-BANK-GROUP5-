package com.abcbank.atm.exception;

import java.math.BigDecimal;

/**
 * InvalidAmountException — Requirement 4: Exception Handling
 *
 * 当交易金额无效（≤ 0 或格式错误）时抛出。
 */
public class InvalidAmountException extends Exception {

    private final BigDecimal invalidAmount;

    public InvalidAmountException(BigDecimal invalidAmount) {
        super(String.format("[错误] 交易金额无效: $%s — 金额必须大于零!",
                invalidAmount != null
                        ? invalidAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                        : "null"));
        this.invalidAmount = invalidAmount;
    }

    public InvalidAmountException(String message) {
        super(message);
        this.invalidAmount = null;
    }

    public BigDecimal getInvalidAmount() {
        return invalidAmount;
    }
}

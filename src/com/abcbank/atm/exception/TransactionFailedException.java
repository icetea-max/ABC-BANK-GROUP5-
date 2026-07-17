package com.abcbank.atm.exception;

/**
 * TransactionFailedException — Requirement 4: Exception Handling
 *
 * 通用交易失败异常，用于处理未知交易类型或其他无法归类的交易失败场景。
 * 可包装其他异常的 cause。
 */
public class TransactionFailedException extends Exception {

    private final String reason;

    public TransactionFailedException(String reason) {
        super("[错误] 交易失败: " + reason);
        this.reason = reason;
    }

    public TransactionFailedException(String reason, Throwable cause) {
        super("[错误] 交易失败: " + reason, cause);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}

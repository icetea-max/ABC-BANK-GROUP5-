package com.abcbank.atm.exception;

/**
 * AccountNotFoundException — Requirement 4: Exception Handling
 *
 * 当指定的客户账户在系统中不存在时抛出。
 * 替换原有的 -1 哨兵值 (BigDecimal.valueOf(-1))。
 */
public class AccountNotFoundException extends Exception {

    private final String customerId;
    private final String accountNumber;

    public AccountNotFoundException(String customerId, String accountNumber) {
        super(String.format("[提示] 客户ID %s 的账户 %s 不存在!",
                customerId, accountNumber));
        this.customerId = customerId;
        this.accountNumber = accountNumber;
    }

    public AccountNotFoundException(String message) {
        super(message);
        this.customerId = null;
        this.accountNumber = null;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}

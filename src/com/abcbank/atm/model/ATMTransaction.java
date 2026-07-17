package com.abcbank.atm.model;

import com.abcbank.atm.enums.TransactionStatus;
import com.abcbank.atm.exception.InsufficientBalanceException;
import com.abcbank.atm.exception.InvalidAmountException;
import com.abcbank.atm.exception.TransactionFailedException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ATMTransaction 类 - 第5组 ATM提款系统
 *
 * 字段:
 * - Customer ID (客户ID)
 * - Account Number (账户号码)
 * - Transaction Amount (交易金额) — 使用 BigDecimal 精确表示
 * - Transaction Type (交易类型)
 * - Status (状态: TransactionStatus 枚举 — Requirement 1)
 *
 * 同时包含账户余额用于判断提款是否成功
 *
 * 【审计合规升级】
 * - Requirement 1: 使用 TransactionStatus 枚举替换硬编码字符串
 * - Requirement 4: 使用异常处理替换 silently failing
 */
public class ATMTransaction {

    // ============ 成员变量 (Variables) ============
    private String customerId;               // 客户ID
    private String accountNumber;            // 账户号码
    private BigDecimal transactionAmount;    // 交易金额 (BigDecimal 避免浮点精度问题)
    private String transactionType;          // 交易类型
    private TransactionStatus status;        // 交易状态 (Requirement 1: Enum 替换 String)
    private BigDecimal accountBalance;       // 账户余额 (BigDecimal 避免浮点精度问题)

    // ============ 构造方法 (Constructor) ============

    /**
     * 无参构造 — 默认状态设为 REJECTED
     */
    public ATMTransaction() {
        this.customerId = "";
        this.accountNumber = "";
        this.transactionAmount = BigDecimal.ZERO;
        this.transactionType = "";
        this.status = TransactionStatus.REJECTED;
        this.accountBalance = BigDecimal.ZERO;
    }

    /**
     * 全参构造 — 新交易默认状态设为 PENDING（待处理）
     *
     * @throws IllegalArgumentException 当必填字段为 null 时抛出（空值防护）
     */
    public ATMTransaction(String customerId, String accountNumber,
                          BigDecimal transactionAmount, String transactionType,
                          BigDecimal accountBalance) {
        // Requirement 4: 空值防护 — 构造函数参数校验
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("客户ID 不能为空");
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("账户号码 不能为空");
        }
        if (transactionAmount == null) {
            throw new IllegalArgumentException("交易金额 不能为 null");
        }
        if (transactionType == null || transactionType.trim().isEmpty()) {
            throw new IllegalArgumentException("交易类型 不能为空");
        }
        if (accountBalance == null) {
            throw new IllegalArgumentException("账户余额 不能为 null");
        }

        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.transactionAmount = transactionAmount;
        this.transactionType = transactionType;
        this.accountBalance = accountBalance;
        // Requirement 1: 新交易使用 PENDING 状态
        this.status = TransactionStatus.PENDING;
    }

    // ============ Getter 和 Setter 方法 (Methods) ============

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Requirement 1: 返回 TransactionStatus 枚举（替代 String）
     */
    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = accountBalance;
    }

    // ============ 业务方法 (Business Methods) ============

    /**
     * 处理交易 — Requirement 4: 使用异常处理替代 silently failing
     *
     * Withdrawal:
     *   余额足够 -> status = COMPLETED (扣款)
     *   余额不足 -> throw InsufficientBalanceException
     *
     * Deposit:
     *   始终成功 -> status = COMPLETED (余额增加)
     *
     * Balance Inquiry:
     *   始终成功 -> status = COMPLETED (余额不变)
     *
     * @throws InvalidAmountException       金额 ≤ 0 时抛出
     * @throws InsufficientBalanceException 余额不足时抛出
     * @throws TransactionFailedException   交易类型未知或其他失败时抛出
     */
    public void processTransaction()
            throws InvalidAmountException, InsufficientBalanceException, TransactionFailedException {

        // Requirement 4: 金额校验 — 无效金额抛异常（替代 silently setting REJECTED）
        if (transactionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(transactionAmount);
        }

        // switch 根据交易类型执行不同逻辑 (PDF要求)
        switch (transactionType) {
            case "Withdrawal":
                // 使用 compareTo 精确比较
                if (accountBalance.compareTo(transactionAmount) >= 0) {
                    accountBalance = accountBalance.subtract(transactionAmount);
                    status = TransactionStatus.COMPLETED;  // Requirement 1: Enum 常量
                    System.out.println("  [成功] " + transactionType
                            + " $" + formatAmount(transactionAmount) + " 已完成。");
                } else {
                    // Requirement 4: 余额不足 — 抛异常替代 silently setting REJECTED
                    throw new InsufficientBalanceException(accountBalance, transactionAmount);
                }
                break;

            case "Deposit":
                // 存款始终成功，余额增加
                accountBalance = accountBalance.add(transactionAmount);
                status = TransactionStatus.COMPLETED;
                System.out.println("  [成功] 存款 $" + formatAmount(transactionAmount)
                        + " 已完成。新余额: $" + formatAmount(accountBalance));
                break;

            case "Balance Inquiry":
                // 余额查询始终成功，余额不变
                status = TransactionStatus.COMPLETED;
                System.out.println("  [成功] 余额查询完成。当前余额: $" + formatAmount(accountBalance));
                break;

            default:
                // Requirement 4: 未知交易类型 — 抛异常
                throw new TransactionFailedException("未知交易类型: " + transactionType);
        }
    }

    /**
     * 显示单条交易记录详情
     */
    public void displayTransaction() {
        System.out.println("| " + padRight(customerId, 12)
                + "| " + padRight(accountNumber, 14)
                + "| $" + padRight(formatAmount(transactionAmount), 10)
                + "| " + padRight(transactionType, 18)
                + "| " + padRight(formatAmount(accountBalance), 12)
                + "| " + padRight(status.getDisplayName(), 12) + "|");
    }

    /**
     * 格式化金额显示 — 保留两位小数，使用 HALF_UP 舍入模式（四舍五入）
     * toPlainString() 避免科学计数法
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 辅助方法: 字符串右填充，用于格式化表格输出
     */
    private String padRight(String str, int length) {
        if (str == null || str.length() >= length) {
            return str == null ? "" : str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(" ");
        }
        return sb.toString();
    }
}

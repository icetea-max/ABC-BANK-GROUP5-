package com.abcbank.atm;

/**
 * ATMTransaction 类 - 第5组 ATM提款系统
 *
 * 字段:
 * - Customer ID (客户ID)
 * - Account Number (账户号码)
 * - Transaction Amount (交易金额)
 * - Transaction Type (交易类型)
 * - Status (状态: Successful / Rejected)
 *
 * 同时包含账户余额用于判断提款是否成功
 */
public class ATMTransaction {

    // ============ 成员变量 (Variables) ============
    private String customerId;        // 客户ID
    private String accountNumber;     // 账户号码
    private double transactionAmount; // 交易金额
    private String transactionType;   // 交易类型
    private String status;            // 交易状态: Successful / Rejected
    private double accountBalance;    // 账户余额

    // ============ 构造方法 (Constructor) ============
    public ATMTransaction() {
        this.customerId = "";
        this.accountNumber = "";
        this.transactionAmount = 0.0;
        this.transactionType = "";
        this.status = "Rejected";
        this.accountBalance = 0.0;
    }

    public ATMTransaction(String customerId, String accountNumber,
                          double transactionAmount, String transactionType,
                          double accountBalance) {
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.transactionAmount = transactionAmount;
        this.transactionType = transactionType;
        this.accountBalance = accountBalance;
        // 默认状态为 Rejected，处理成功后更新为 Successful
        this.status = "Rejected";
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

    public double getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }

    // ============ 业务方法 (Business Methods) ============

    /**
     * 处理交易 - 使用 switch 区分交易类型，if/else 判断 Successful / Rejected
     *
     * Withdrawal / Fast Cash / Transfer:
     *   余额足够 -> Successful (扣款)
     *   余额不足 -> Rejected
     *
     * Deposit:
     *   始终 Successful (余额增加)
     *
     * Balance Inquiry:
     *   始终 Successful (余额不变)
     */
    public void processTransaction() {
        // 金额校验 (if/else)
        if (transactionAmount <= 0) {
            status = "Rejected";
            System.out.println("  [错误] 交易金额必须大于零!");
            return;
        }

        // switch 根据交易类型执行不同逻辑 (PDF要求)
        switch (transactionType) {
            case "Withdrawal":
            case "Fast Cash":
            case "Transfer":
                // if/else - 判断余额是否足够 (PDF核心要求)
                if (accountBalance >= transactionAmount) {
                    accountBalance -= transactionAmount;
                    status = "Successful";
                    System.out.println("  [成功] " + transactionType
                            + " $" + transactionAmount + " 已完成。");
                } else {
                    status = "Rejected";
                    System.out.println("  [拒绝] 余额不足! 当前余额: $" + accountBalance
                            + ", 交易金额: $" + transactionAmount);
                }
                break;

            case "Deposit":
                // 存款始终成功，余额增加
                accountBalance += transactionAmount;
                status = "Successful";
                System.out.println("  [成功] 存款 $" + transactionAmount
                        + " 已完成。新余额: $" + accountBalance);
                break;

            case "Balance Inquiry":
                // 余额查询始终成功，余额不变
                status = "Successful";
                System.out.println("  [成功] 余额查询完成。当前余额: $" + accountBalance);
                break;

            default:
                status = "Rejected";
                System.out.println("  [错误] 未知交易类型: " + transactionType);
        }
    }

    /**
     * 显示单条交易记录详情
     */
    public void displayTransaction() {
        System.out.println("| " + padRight(customerId, 12)
                + "| " + padRight(accountNumber, 14)
                + "| $" + padRight(String.format("%.2f", transactionAmount), 10)
                + "| " + padRight(transactionType, 18)
                + "| " + padRight(String.format("%.2f", accountBalance), 12)
                + "| " + padRight(status, 12) + "|");
    }

    /**
     * 辅助方法: 字符串右填充，用于格式化表格输出
     */
    private String padRight(String str, int length) {
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(" ");
        }
        return sb.toString();
    }
}

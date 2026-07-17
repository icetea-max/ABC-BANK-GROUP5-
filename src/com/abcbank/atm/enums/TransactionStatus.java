package com.abcbank.atm.enums;

/**
 * TransactionStatus 枚举 — Requirement 1: Enum 替换 Hard-coded String
 *
 * PDF 规定的 5 个枚举值:
 *   ACTIVE    — 活跃状态（账户处于正常使用中）
 *   INACTIVE  — 非活跃状态（账户已停用/冻结）
 *   COMPLETED — 已完成（交易成功处理）
 *   PENDING   — 待处理（交易已创建但尚未处理）
 *   REJECTED  — 已拒绝（交易被拒绝）
 *
 * 替换原有的硬编码字符串 "Successful" / "Rejected"
 * 映射: "Successful" → COMPLETED, "Rejected" → REJECTED
 */
public enum TransactionStatus {

    ACTIVE("Active"),
    INACTIVE("Inactive"),
    COMPLETED("Completed"),
    PENDING("Pending"),
    REJECTED("Rejected");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 返回用于界面显示的友好名称
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

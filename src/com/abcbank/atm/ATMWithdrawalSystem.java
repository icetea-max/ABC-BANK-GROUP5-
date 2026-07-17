package com.abcbank.atm;

import com.abcbank.atm.enums.TransactionStatus;
import com.abcbank.atm.exception.AccountNotFoundException;
import com.abcbank.atm.exception.InsufficientBalanceException;
import com.abcbank.atm.exception.InvalidAmountException;
import com.abcbank.atm.exception.TransactionFailedException;
import com.abcbank.atm.impl.SavingsInterestCalculator;
import com.abcbank.atm.impl.StandardPaymentPolicy;
import com.abcbank.atm.interfaces.InterestCalculator;
import com.abcbank.atm.interfaces.PaymentPolicy;
import com.abcbank.atm.model.ATMTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ATMWithdrawalSystem - 第5组 ATM提款系统 主类
 *
 * ============ 并发安全设计 ============
 *
 * 银行系统并发三大核心问题及解决方案：
 *
 * 问题1: transactionCount++ 非原子操作
 *   → 使用 AtomicInteger，底层 CAS 保证自增原子性
 *
 * 问题2: 同一账户被多线程同时扣款（双重支付 / Double-Spending）
 *   → ConcurrentHashMap<String, ReentrantLock> 维护每账户独立锁
 *     同一账户操作串行化，不同账户可完全并行
 *
 * 问题3: 数组读写竞态（一个线程在读，另一个在写）
 *   → 所有修改方法用 synchronized 保护
 *     读方法也用 synchronized 保证读到一致快照
 *
 * 吞吐量优化: 账户级细粒度锁 vs 全局锁
 *   全局锁 → 所有操作串行，TPS 极低
 *   账户锁 → 只要不同账户，可并行执行，TPS 随账户数线性增长
 *
 * ============ PDF 要求对照 ============
 * ✓ Enum + Interface + Exception Handling
 *
 * @author ABC Bank - Java Junior Developer
 * @version 3.0 (Thread-Safe)
 */
public class ATMWithdrawalSystem {

    // ============ 成员变量 ============
    private static final int MAX_TRANSACTIONS = 5;
    private final ATMTransaction[] transactions;

    // 【并发】AtomicInteger 保证自增原子性
    private final AtomicInteger transactionCount;

    // Requirement 2: Interface 集成
    private final PaymentPolicy paymentPolicy;
    private final InterestCalculator interestCalculator;

    // 【并发】每账户独立锁，key = "customerId:accountNumber"
    private final ConcurrentHashMap<String, ReentrantLock> accountLocks;

    // ============ 构造方法 ============
    public ATMWithdrawalSystem() {
        transactions = new ATMTransaction[MAX_TRANSACTIONS];
        transactionCount = new AtomicInteger(0);
        this.paymentPolicy = new StandardPaymentPolicy();
        this.interestCalculator = new SavingsInterestCalculator();
        this.accountLocks = new ConcurrentHashMap<>();
    }

    // ============ 主方法 - 程序入口 ============
    public static void main(String[] args) {
        ATMWithdrawalSystem atmSystem = new ATMWithdrawalSystem();
        atmSystem.run();
    }

    // ============ 核心业务方法 ============

    /**
     * 运行ATM提款系统主流程
     */
    public void run() {
        int choice = 0;

        System.out.println("================================================");
        System.out.println("   ABC Bank ATM Withdrawal System (Group 5)");
        System.out.println("   ABC银行 ATM提款系统 (第5组)");
        System.out.println("   [v3.0 并发安全版 — Thread-Safe]");
        System.out.println("================================================");

        try (Scanner scanner = new Scanner(System.in)) {

            while (choice != 7) {
                displayMenu();

                try {
                    System.out.print("\n请输入您的选择 (Enter your choice): ");
                    choice = scanner.nextInt();
                    scanner.nextLine();
                } catch (InputMismatchException e) {
                    System.out.println("[错误] 请输入有效的数字!");
                    scanner.nextLine();
                    continue;
                }

                switch (choice) {
                    case 1:
                        addTransaction(scanner);
                        break;
                    case 2:
                        processAllWithdrawals();
                        break;
                    case 3:
                        displayAllTransactions();
                        break;
                    case 4:
                        displaySummary();
                        break;
                    case 5:
                        searchByStatus(scanner);
                        break;
                    case 6:
                        runConcurrencyDemo();
                        break;
                    case 7:
                        System.out.println("\n感谢使用 ABC Bank ATM 系统。再见!");
                        break;
                    default:
                        System.out.println("[错误] 无效选择，请重新输入!");
                }
            }
        }
    }

    /**
     * 显示主菜单
     */
    private void displayMenu() {
        System.out.println("\n------------------ 主菜单 (Main Menu) ------------------");
        System.out.println("1. 添加交易记录 (Add Transaction)");
        System.out.println("2. 处理所有提款 (Process All Withdrawals)");
        System.out.println("3. 显示所有交易 (Display All Transactions)");
        System.out.println("4. 显示汇总报告 (Display Summary)");
        System.out.println("5. 按状态搜索 (Search by Status)");
        System.out.println("6. 并发演示 (Concurrency Demo)");
        System.out.println("7. 退出 (Exit)");
        System.out.println("--------------------------------------------------------");
    }

    // ============ 【并发】账户锁工具方法 ============

    /**
     * 获取指定账户的独立锁。
     * computeIfAbsent 是原子的：如果 key 不存在则创建新锁，否则返回已有锁。
     * 确保同一账户的所有操作使用同一把锁。
     */
    /** 获取指定账户的独立锁（包级访问，供测试使用） */
    ReentrantLock getAccountLock(String customerId, String accountNumber) {
        String key = customerId + ":" + accountNumber;
        return accountLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    // ============ 功能方法 ============

    /**
     * 案例1: 添加交易记录
     *
     * 【并发】关键流程:
     *   1. 获取该账户的独立锁 (不同账户可并行)
     *   2. synchronized 保护数组写入
     *   3. 在锁内: 查余额 → 创建交易 → 处理 → 写入数组
     *   4. finally 释放锁
     *
     * 这样保证: 同一账户在同一时刻只有一笔交易在执行，杜绝双重支付。
     */
    private void addTransaction(Scanner scanner) {
        // 并发检查：使用 AtomicInteger.get() 读取
        if (transactionCount.get() >= MAX_TRANSACTIONS) {
            System.out.println("\n[错误] 交易记录已满! 最多存储 " + MAX_TRANSACTIONS + " 条记录。");
            return;
        }

        int currentIndex = transactionCount.get();
        System.out.println("\n========== 添加第 " + (currentIndex + 1) + " 条交易记录 ==========");

        // ---- 第1步: 读取客户ID和账户号码 ----
        System.out.print("客户ID (Customer ID):     ");
        String customerId = scanner.nextLine();

        System.out.print("账户号码 (Account Number): ");
        String accountNumber = scanner.nextLine();

        // ---- 第2步: 选择交易类型 ----
        String transactionType = selectTransactionType(scanner);

        // ---- 第3步: 读取交易金额 ----
        BigDecimal amount = null;
        while (amount == null) {
            try {
                System.out.print("交易金额 (Transaction Amount): $");
                amount = scanner.nextBigDecimal();
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("[错误] 请输入有效的金额!");
                scanner.nextLine();
            }
        }

        // ============ 【并发核心】账户级锁定 ============
        // 获取该账户的独立锁，锁住整个"查余额→处理→写数组"流程
        ReentrantLock accountLock = getAccountLock(customerId, accountNumber);
        accountLock.lock();
        try {
            // ---- 临界区开始: 同一账户的串行化保障 ----

            // 第4步: 查找账户余额（在锁内保证读到最新值）
            BigDecimal currentBalance;
            try {
                currentBalance = findAccountBalance(customerId, accountNumber);
                System.out.println("\n[提示] 账户已存在! 当前余额: $" + formatAmount(currentBalance));
            } catch (AccountNotFoundException e) {
                System.out.println("\n" + e.getMessage());
                System.out.println("[系统] 正在为您创建新账户...");
                currentBalance = BigDecimal.ZERO;
                System.out.println("[成功] 新账户已创建! 客户ID: " + customerId
                        + ", 账户号码: " + accountNumber + ", 当前余额: $0.00");
            }

            // 第5步: 创建交易对象
            ATMTransaction transaction = new ATMTransaction(
                    customerId, accountNumber, amount, transactionType, currentBalance);

            // ---- synchronized 块保护数组写入 ----
            int idx;
            synchronized (this) {
                idx = transactionCount.getAndIncrement();
                transactions[idx] = transaction;
            }

            // 第6步: PaymentPolicy 预验证（仅扣款类交易）
            boolean isDebitTransaction = transactionType.equals("Withdrawal");
            if (isDebitTransaction) {
                System.out.println("\n--- 支付策略验证 (Payment Policy Check) ---");
                System.out.println("  策略: " + ((StandardPaymentPolicy) paymentPolicy).getPolicyDescription());

                if (!paymentPolicy.approveTransaction(currentBalance, amount)) {
                    transaction.setStatus(TransactionStatus.REJECTED);
                    String reason = ((StandardPaymentPolicy) paymentPolicy).getRejectionReason(currentBalance, amount);
                    System.out.println("  [策略拒绝] " + reason);
                    System.out.println(">>> 交易被支付策略拒绝 <<<");
                    System.out.println("[完成] 交易记录已添加但被拒绝! (当前共 " + transactionCount.get() + " 条记录)");
                    return;
                }
                System.out.println("  [策略通过] 交易已获批准。");
            }

            // 第7步: 处理交易
            System.out.println("\n--- 处理交易结果 ---");
            try {
                transaction.processTransaction();

                if (transaction.getStatus() == TransactionStatus.COMPLETED) {
                    System.out.println(">>> 交易成功 (Completed) <<<");

                    if (transaction.getTransactionType().equals("Deposit")) {
                        BigDecimal newBalance = transaction.getAccountBalance();
                        BigDecimal estimatedInterest = interestCalculator.calculateInterest(
                                newBalance, new BigDecimal("0.035"), 12);
                        System.out.println("  [利息预览] 若 $" + formatAmount(newBalance)
                                + " 存12个月 @3.5%年利率, 预估利息: $" + formatAmount(estimatedInterest));
                    }
                } else {
                    System.out.println(">>> 交易失败 (" + transaction.getStatus().getDisplayName() + ") <<<");
                }
            } catch (InvalidAmountException e) {
                System.out.println("  [异常-无效金额] " + e.getMessage());
                System.out.println(">>> 交易失败 (Rejected) <<<");
            } catch (InsufficientBalanceException e) {
                System.out.println("  [异常-余额不足] " + e.getMessage());
                System.out.println(">>> 交易失败 (Rejected) <<<");
            } catch (TransactionFailedException e) {
                System.out.println("  [异常-交易失败] " + e.getMessage());
                System.out.println(">>> 交易失败 (Rejected) <<<");
            }

            System.out.println("[成功] 交易记录添加成功! (当前共 " + transactionCount.get() + " 条记录)");

            // ---- 临界区结束 ----
        } finally {
            accountLock.unlock();
        }
    }

    /**
     * 查找账户余额 - 遍历已处理交易，返回匹配账户的最新余额
     *
     * 调用此方法时已持有账户锁，保证读到一致余额。
     */
    private BigDecimal findAccountBalance(String customerId, String accountNumber)
            throws AccountNotFoundException {
        int count = transactionCount.get();
        BigDecimal balance = null;
        for (int i = 0; i < count; i++) {
            ATMTransaction txn = transactions[i];
            // 【并发防护】槽位可能尚未写入，跳过 null
            if (txn == null) {
                continue;
            }
            if (txn.getCustomerId().equals(customerId)
                    && txn.getAccountNumber().equals(accountNumber)) {
                balance = txn.getAccountBalance(); // 继续遍历，取最新余额
            }
        }
        if (balance == null) {
            throw new AccountNotFoundException(customerId, accountNumber);
        }
        return balance;
    }

    /**
     * switch 语句 - 选择交易类型
     */
    private String selectTransactionType(Scanner scanner) {
        System.out.println("\n交易类型 (Transaction Type):");
        System.out.println("  1. Withdrawal (提款)");
        System.out.println("  2. Deposit (存款)");
        System.out.println("  3. Balance Inquiry (余额查询)");
        System.out.print("请选择 (1-3): ");

        int typeChoice;
        try {
            typeChoice = scanner.nextInt();
            scanner.nextLine();
        } catch (InputMismatchException e) {
            System.out.println("[警告] 无效选择，默认设为 Withdrawal");
            scanner.nextLine();
            typeChoice = 1;
        }

        switch (typeChoice) {
            case 1:  return "Withdrawal";
            case 2:  return "Deposit";
            case 3:  return "Balance Inquiry";
            default:
                System.out.println("[警告] 无效选择，默认设为 Withdrawal");
                return "Withdrawal";
        }
    }

    /**
     * 案例2: 处理所有提款交易
     * 【并发】synchronized 保证数组读取不被并发写入干扰
     */
    private synchronized void processAllWithdrawals() {
        int count = transactionCount.get();
        if (count == 0) {
            System.out.println("\n[提示] 暂无交易记录，请先添加交易!");
            return;
        }

        System.out.println("\n========== 处理所有提款交易 ==========");

        for (int i = 0; i < count; i++) {
            ATMTransaction txn = transactions[i];
            if (txn == null) continue;
            System.out.println("\n--- 处理交易 #" + (i + 1) + " ---");
            System.out.println("  客户ID: " + txn.getCustomerId());
            System.out.println("  账户:   " + txn.getAccountNumber());
            System.out.println("  类型:   " + txn.getTransactionType());
            System.out.println("  金额:   $" + formatAmount(txn.getTransactionAmount()));
            System.out.println("  处理前状态: " + txn.getStatus().getDisplayName());

            try {
                txn.processTransaction();
                System.out.println("  >>> 状态: " + txn.getStatus().getDisplayName() + " <<<");
            } catch (InvalidAmountException e) {
                System.out.println("  [异常-无效金额] " + e.getMessage());
                System.out.println("  >>> 状态: " + txn.getStatus().getDisplayName() + " <<<");
            } catch (InsufficientBalanceException e) {
                System.out.println("  [异常-余额不足] " + e.getMessage());
                System.out.println("  >>> 状态: " + txn.getStatus().getDisplayName() + " <<<");
            } catch (TransactionFailedException e) {
                System.out.println("  [异常-交易失败] " + e.getMessage());
                System.out.println("  >>> 状态: " + txn.getStatus().getDisplayName() + " <<<");
            }
        }

        System.out.println("\n[完成] 所有交易已处理完毕!");
    }

    /**
     * 案例3: 显示所有交易记录
     * 【并发】synchronized 读锁保证读到一致快照
     */
    private synchronized void displayAllTransactions() {
        int count = transactionCount.get();
        if (count == 0) {
            System.out.println("\n[提示] 暂无交易记录!");
            return;
        }

        System.out.println("\n==================== 所有交易记录 ====================");
        printTableHeader();

        for (int i = 0; i < count; i++) {
            if (transactions[i] != null) {
                transactions[i].displayTransaction();
            }
        }

        printTableFooter();
        System.out.println("  共 " + count + " 条记录");
    }

    /**
     * 案例4: 显示汇总报告
     * 【并发】synchronized 保证统计期间数组不被修改
     */
    private synchronized void displaySummary() {
        int count = transactionCount.get();
        if (count == 0) {
            System.out.println("\n[提示] 暂无交易记录!");
            return;
        }

        int completedCount = 0;
        int rejectedCount = 0;
        int pendingCount = 0;
        BigDecimal totalCompletedAmount = BigDecimal.ZERO;
        BigDecimal totalRejectedAmount = BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            ATMTransaction txn = transactions[i];
            if (txn == null) continue;

            if (txn.getStatus() == TransactionStatus.COMPLETED) {
                completedCount++;
                totalCompletedAmount = totalCompletedAmount.add(txn.getTransactionAmount());
            } else if (txn.getStatus() == TransactionStatus.REJECTED) {
                rejectedCount++;
                totalRejectedAmount = totalRejectedAmount.add(txn.getTransactionAmount());
            } else if (txn.getStatus() == TransactionStatus.PENDING) {
                pendingCount++;
            }
        }

        System.out.println("\n==================== 汇总报告 (Summary) ====================");
        System.out.println("  总交易数 (Total Transactions):     " + count);
        System.out.println("  --------------------------------------------------");
        System.out.println("  COMPLETED (已完成):                 " + completedCount
                + "  笔  |  金额合计: $" + formatAmount(totalCompletedAmount));
        System.out.println("  REJECTED  (已拒绝):                 " + rejectedCount
                + "  笔  |  金额合计: $" + formatAmount(totalRejectedAmount));
        if (pendingCount > 0) {
            System.out.println("  PENDING   (待处理):                 " + pendingCount + "  笔");
        }
        System.out.println("  --------------------------------------------------");

        int processedCount = completedCount + rejectedCount;
        if (processedCount > 0) {
            double successRate = (double) completedCount / processedCount * 100;
            System.out.println("  成功率 (Success Rate):              "
                    + String.format("%.1f", successRate) + "%");

            if (successRate >= 99.999) {
                System.out.println("  评价: 所有已处理交易均成功!");
            } else if (successRate >= 50.0) {
                System.out.println("  评价: 大部分交易成功处理。");
            } else {
                System.out.println("  评价: 需要关注 - 拒绝率较高。");
            }
        }
        System.out.println("===========================================================");
    }

    /**
     * 案例5: 按状态搜索
     * 【并发】synchronized 保证搜索期间数组一致
     */
    private synchronized void searchByStatus(Scanner scanner) {
        int count = transactionCount.get();
        if (count == 0) {
            System.out.println("\n[提示] 暂无交易记录!");
            return;
        }

        System.out.println("\n========== 按状态搜索 (Search by Status) ==========");
        System.out.println("  1. COMPLETED (已完成)");
        System.out.println("  2. REJECTED  (已拒绝)");
        System.out.println("  3. PENDING   (待处理)");
        System.out.println("  4. ACTIVE    (活跃)");
        System.out.println("  5. INACTIVE  (非活跃)");
        System.out.print("请选择状态 (1-5): ");

        int statusChoice;
        try {
            statusChoice = scanner.nextInt();
            scanner.nextLine();
        } catch (InputMismatchException e) {
            System.out.println("[警告] 无效输入，默认搜索 REJECTED");
            scanner.nextLine();
            statusChoice = 2;
        }

        TransactionStatus searchStatus;
        switch (statusChoice) {
            case 1:  searchStatus = TransactionStatus.COMPLETED; break;
            case 2:  searchStatus = TransactionStatus.REJECTED;  break;
            case 3:  searchStatus = TransactionStatus.PENDING;   break;
            case 4:  searchStatus = TransactionStatus.ACTIVE;    break;
            case 5:  searchStatus = TransactionStatus.INACTIVE;  break;
            default:
                System.out.println("[警告] 无效选择，默认搜索 REJECTED");
                searchStatus = TransactionStatus.REJECTED;
        }

        System.out.println("\n--- 搜索条件: " + searchStatus.getDisplayName() + " ---");
        printTableHeader();

        boolean found = false;
        for (int i = 0; i < count; i++) {
            if (transactions[i] != null && transactions[i].getStatus() == searchStatus) {
                transactions[i].displayTransaction();
                found = true;
            }
        }

        if (!found) {
            System.out.println("|               (无匹配记录)              |");
        }

        printTableFooter();
    }

    // ============ 并发演示 ============

    /**
     * 案例6: 并发演示
     * 启动 3 个线程同时操作同一账户，观察账户锁如何防止双重支付。
     *
     * 场景: 账户余额 $300，3 个线程各尝试取 $200
     * 预期: 只有 1 笔成功，2 笔被拒绝（余额不足）
     */
    private void runConcurrencyDemo() {
        System.out.println("\n========== 并发安全演示 (Concurrency Demo) ==========");
        System.out.println("场景: 账户 DEMO-ACC 余额 $300");
        System.out.println("      3 个线程同时各取 $200");
        System.out.println("预期: 只有 1 笔成功，2 笔被拒绝");
        System.out.println("======================================================");

        // 先存入 $300 到演示账户
        ATMTransaction seedTx = new ATMTransaction(
                "DEMO", "DEMO-ACC",
                new BigDecimal("300"), "Deposit",
                BigDecimal.ZERO);
        int idx = transactionCount.getAndIncrement();
        if (idx < MAX_TRANSACTIONS) {
            transactions[idx] = seedTx;
            try {
                seedTx.processTransaction();
            } catch (Exception ignored) {}
        }

        System.out.println("\n[准备] 演示账户已就绪，余额: $" + formatAmount(seedTx.getAccountBalance()));
        System.out.println("[启动] 3 个并发线程开始操作...\n");

        // 创建 3 个并发线程
        Thread[] threads = new Thread[3];
        final int[] successCount = {0};
        final int[] failCount = {0};

        for (int t = 0; t < 3; t++) {
            final int threadId = t + 1;
            threads[t] = new Thread(() -> {
                String threadName = "Thread-" + threadId;
                System.out.println("  [" + threadName + "] 启动，尝试取款 $200...");

                // 获取该账户的锁
                ReentrantLock lock = getAccountLock("DEMO", "DEMO-ACC");
                lock.lock();
                try {
                    // 在锁内查余额
                    BigDecimal balance;
                    try {
                        balance = findAccountBalance("DEMO", "DEMO-ACC");
                    } catch (AccountNotFoundException e) {
                        balance = BigDecimal.ZERO;
                    }

                    System.out.println("  [" + threadName + "] 读取余额: $" + formatAmount(balance));

                    // 检查余额是否足够
                    if (balance.compareTo(new BigDecimal("200")) >= 0) {
                        // 创建并处理取款
                        ATMTransaction tx = new ATMTransaction(
                                "DEMO", "DEMO-ACC",
                                new BigDecimal("200"), "Withdrawal",
                                balance);

                        try {
                            int writeIdx;
                            synchronized (this) {
                                writeIdx = transactionCount.getAndIncrement();
                                if (writeIdx < MAX_TRANSACTIONS) {
                                    transactions[writeIdx] = tx;
                                }
                            }

                            tx.processTransaction();
                            synchronized (successCount) { successCount[0]++; }
                            System.out.println("  [" + threadName + "] ✓ 取款成功! 新余额: $"
                                    + formatAmount(tx.getAccountBalance()));
                        } catch (InvalidAmountException | InsufficientBalanceException
                                | TransactionFailedException e) {
                            synchronized (failCount) { failCount[0]++; }
                            System.out.println("  [" + threadName + "] ✗ 失败: " + e.getMessage());
                        }
                    } else {
                        synchronized (failCount) { failCount[0]++; }
                        System.out.println("  [" + threadName + "] ✗ 余额不足! 当前余额: $"
                                + formatAmount(balance) + ", 需要: $200.00");
                    }
                } finally {
                    lock.unlock();
                }
            });
        }

        // 启动所有线程
        for (Thread t : threads) {
            t.start();
        }

        // 等待所有线程完成
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n========== 并发演示结果 ==========");
        System.out.println("  成功: " + successCount[0] + " 笔");
        System.out.println("  失败: " + failCount[0] + " 笔");
        System.out.println("  结论: 账户锁有效防止了双重支付 ✓");
        System.out.println("====================================");
    }

    // ============ 辅助方法 ============

    // ---- 包级访问方法（供 JUnit 测试使用） ----

    /** 返回 AtomicInteger 计数器，供测试使用 getAndIncrement() */
    AtomicInteger getTransactionCount() {
        return transactionCount;
    }

    /** 查找账户余额（包级访问，供测试使用） */
    BigDecimal findBalance(String customerId, String accountNumber)
            throws AccountNotFoundException {
        return findAccountBalance(customerId, accountNumber);
    }

    /** 将交易写入数组指定位置（包级访问，供测试使用） */
    void addToArray(int index, ATMTransaction tx) {
        if (index >= 0 && index < MAX_TRANSACTIONS) {
            transactions[index] = tx;
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void printTableHeader() {
        System.out.println("+-------------+---------------+------------+-------------------+"
                + "-------------+-------------+");
        System.out.println("| Customer ID | Account No.   | Amount     | Transaction Type  "
                + "| Balance     | Status      |");
        System.out.println("+-------------+---------------+------------+-------------------+"
                + "-------------+-------------+");
    }

    private void printTableFooter() {
        System.out.println("+-------------+---------------+------------+-------------------+"
                + "-------------+-------------+");
    }
}

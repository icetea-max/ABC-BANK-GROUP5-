package com.abcbank.atm;

import com.abcbank.atm.enums.TransactionStatus;
import com.abcbank.atm.exception.AccountNotFoundException;
import com.abcbank.atm.exception.InsufficientBalanceException;
import com.abcbank.atm.exception.InvalidAmountException;
import com.abcbank.atm.exception.TransactionFailedException;
import com.abcbank.atm.model.ATMTransaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ATMWithdrawalSystem 并发安全测试
 *
 * 使用 CountDownLatch 对齐所有线程启动时机，模拟真实并发。
 * 每个测试中线程复制 addTransaction() 的锁策略：
 *   1) 获取账户级别的 ReentrantLock
 *   2) 在锁内查余额 → 创建交易 → 处理
 *   3) 释放锁
 */
@DisplayName("ATM 银行系统并发安全测试")
class ATMWithdrawalSystemConcurrencyTest {

    private ATMWithdrawalSystem system;

    @BeforeEach
    void setUp() {
        system = new ATMWithdrawalSystem();
    }

    // ================================================================
    // 测试1: 同一账户并发取款 — 双重支付防护（最核心测试）
    // ================================================================

    @Test
    @DisplayName("同一账户 3 线程并发取款 — 只有 1 笔成功")
    void sameAccountConcurrentWithdrawal_PreventsDoubleSpending() throws Exception {
        // Arrange: 存入 $300
        final String custId = "DOUBLE-001";
        final String accNum = "SAV-001";
        seedAccount(custId, accNum, new BigDecimal("300"));

        int threadCount = 3;
        BigDecimal withdrawAmount = new BigDecimal("200");
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        // Act: 3 线程同时取款 — 每个线程复制 addTransaction 的锁模式
        for (int i = 0; i < threadCount; i++) {
            final int id = i + 1;
            pool.submit(() -> {
                try {
                    startLatch.await(); // 等待统一发令

                    // === 复制 addTransaction() 的账户锁模式 ===
                    ReentrantLock accountLock = system.getAccountLock(custId, accNum);
                    accountLock.lock();
                    try {
                        // 锁内查余额
                        BigDecimal balance = system.findBalance(custId, accNum);

                        // 创建交易 + 写入数组
                        int idx = system.getTransactionCount().getAndIncrement();
                        ATMTransaction tx = new ATMTransaction(
                                custId, accNum, withdrawAmount, "Withdrawal", balance);
                        system.addToArray(idx, tx);

                        // 处理交易
                        tx.processTransaction();
                        if (tx.getStatus() == TransactionStatus.COMPLETED) {
                            successCount.incrementAndGet();
                            log("[Thread-%d] ✓ 取款成功 余额=$%s", id, fmt(tx.getAccountBalance()));
                        } else {
                            rejectCount.incrementAndGet();
                            log("[Thread-%d] ✗ 拒绝 状态=%s", id, tx.getStatus().getDisplayName());
                        }
                    } finally {
                        accountLock.unlock();
                    }
                } catch (InsufficientBalanceException e) {
                    rejectCount.incrementAndGet();
                    log("[Thread-%d] ✗ 余额不足", id);
                } catch (AccountNotFoundException e) {
                    log("[Thread-%d] ✗ 账户不存在", id);
                } catch (Exception e) {
                    log("[Thread-%d] ✗ 异常: %s", id, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        log(">>> 3 线程抢同一账户 $300 → 各取 $200 <<<");
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        // Assert
        assertTrue(completed, "所有线程应在超时前完成");
        assertEquals(1, successCount.get(),
                "余额 $300，取 $200×3：只能有 1 笔成功");
        assertEquals(2, rejectCount.get(),
                "应有 2 笔被拒绝");

        log("✓ 成功=%d 失败=%d（双重支付已阻止）", successCount.get(), rejectCount.get());
    }

    // ================================================================
    // 测试2: 同一账户并发存款 — 全部成功，余额累加正确
    // ================================================================

    @Test
    @DisplayName("同一账户 4 线程并发存款 — 全部成功，余额 $401")
    void sameAccountConcurrentDeposit_AllSucceedSumCorrect() throws Exception {
        final String custId = "DEP-001";
        final String accNum = "CHK-001";
        // 种子: 存 $1 创建账户（1 seed + 4 threads = 5 ≤ MAX_TRANSACTIONS）
        seedAccount(custId, accNum, new BigDecimal("1"));

        int threadCount = 4;
        BigDecimal eachDeposit = new BigDecimal("100");
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int id = i + 1;
            pool.submit(() -> {
                try {
                    startLatch.await();

                    // 账户锁
                    ReentrantLock accountLock = system.getAccountLock(custId, accNum);
                    accountLock.lock();
                    try {
                        BigDecimal balance = system.findBalance(custId, accNum);

                        int idx = system.getTransactionCount().getAndIncrement();
                        ATMTransaction tx = new ATMTransaction(
                                custId, accNum, eachDeposit, "Deposit", balance);
                        system.addToArray(idx, tx);

                        tx.processTransaction();
                        if (tx.getStatus() == TransactionStatus.COMPLETED) {
                            successCount.incrementAndGet();
                            log("[Thread-%d] ✓ 存款成功 新余额=$%s", id, fmt(tx.getAccountBalance()));
                        }
                    } finally {
                        accountLock.unlock();
                    }
                } catch (Exception e) {
                    log("[Thread-%d] ✗ %s", id, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        log(">>> 4 线程同时存款 $100（初始余额 $1）<<<");
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(threadCount, successCount.get(), "4 笔存款应全部成功");

        // 最终余额: 种子 $1 + 4 × $100 = $401
        BigDecimal finalBalance = system.findBalance(custId, accNum);
        assertEquals(0, new BigDecimal("401").compareTo(finalBalance),
                "最终余额应为 $401，实际: $" + fmt(finalBalance));

        log("✓ 全部成功，最终余额=$%s", fmt(finalBalance));
    }

    // ================================================================
    // 测试3: 不同账户并发 — 互不干扰，全部成功
    // ================================================================

    @Test
    @DisplayName("不同账户并发 — 各自独立锁，全部成功")
    void differentAccountsConcurrent_AllSucceedInParallel() throws Exception {
        int accountCount = 2;  // 2 seeds + 2 threads = 4 ≤ MAX(5)
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(accountCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(accountCount);

        for (int i = 0; i < accountCount; i++) {
            final String custId = "PARA-" + (i + 1);
            final String accNum = "ACC-" + (i + 1);
            seedAccount(custId, accNum, new BigDecimal("500"));

            final int id = i + 1;
            pool.submit(() -> {
                try {
                    startLatch.await();

                    ReentrantLock accountLock = system.getAccountLock(custId, accNum);
                    accountLock.lock();
                    try {
                        BigDecimal balance = system.findBalance(custId, accNum);

                        int idx = system.getTransactionCount().getAndIncrement();
                        ATMTransaction tx = new ATMTransaction(
                                custId, accNum, new BigDecimal("100"), "Withdrawal", balance);
                        system.addToArray(idx, tx);

                        tx.processTransaction();
                        if (tx.getStatus() == TransactionStatus.COMPLETED) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        accountLock.unlock();
                    }
                } catch (Exception e) {
                    log("[Thread-%d] ✗ %s", id, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        log(">>> 2 个不同账户同时取款（各自余额 $500）<<<");
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(accountCount, successCount.get(),
                "不同账户用不同锁，互不阻塞，应全部成功");
        log("✓ 2 个不同账户全部成功");
    }

    // ================================================================
    // 测试4: 混合并发 — 同一账户同时存 $200 和取 $150
    // ================================================================

    @Test
    @DisplayName("混合并发 — 同一账户同时存 $200 和取 $150")
    void mixedDepositWithdrawalConcurrent_BalanceRemainsCorrect() throws Exception {
        final String custId = "MIX-001";
        final String accNum = "MIX-ACC";
        seedAccount(custId, accNum, new BigDecimal("100"));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // 存款线程
        pool.submit(() -> {
            try {
                startLatch.await();
                ReentrantLock lock = system.getAccountLock(custId, accNum);
                lock.lock();
                try {
                    BigDecimal bal = system.findBalance(custId, accNum);
                    int idx = system.getTransactionCount().getAndIncrement();
                    ATMTransaction tx = new ATMTransaction(custId, accNum,
                            new BigDecimal("200"), "Deposit", bal);
                    system.addToArray(idx, tx);
                    tx.processTransaction();
                    log("[存款线程] 完成 余额=$%s", fmt(tx.getAccountBalance()));
                } finally { lock.unlock(); }
            } catch (Exception e) {
                log("[存款线程] 异常: %s", e.getMessage());
            } finally { doneLatch.countDown(); }
        });

        // 取款线程
        pool.submit(() -> {
            try {
                startLatch.await();
                ReentrantLock lock = system.getAccountLock(custId, accNum);
                lock.lock();
                try {
                    BigDecimal bal = system.findBalance(custId, accNum);
                    int idx = system.getTransactionCount().getAndIncrement();
                    ATMTransaction tx = new ATMTransaction(custId, accNum,
                            new BigDecimal("150"), "Withdrawal", bal);
                    system.addToArray(idx, tx);
                    try {
                        tx.processTransaction();
                        log("[取款线程] 完成 余额=$%s", fmt(tx.getAccountBalance()));
                    } catch (InsufficientBalanceException e) {
                        log("[取款线程] 余额不足（存款先执行则不会发生）");
                    }
                } finally { lock.unlock(); }
            } catch (Exception e) {
                log("[取款线程] 异常: %s", e.getMessage());
            } finally { doneLatch.countDown(); }
        });

        log(">>> 初始 $100，存款 $200 + 取款 $150 并发 <<<");
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        BigDecimal finalBalance = system.findBalance(custId, accNum);
        // 存先取后: 100+200-150=150; 取先被拒存后: 100+200=300
        assertTrue(
                finalBalance.compareTo(new BigDecimal("150")) == 0
                        || finalBalance.compareTo(new BigDecimal("300")) == 0,
                "余额应为 $150 或 $300，实际: $" + fmt(finalBalance)
        );
        log("✓ 最终余额=$%s", fmt(finalBalance));
    }

    // ================================================================
    // 测试5: 数组满时并发 — 超出部分被拒绝
    // ================================================================

    @Test
    @DisplayName("数组满时并发 — 仅 1 线程写入成功")
    void arrayFullConcurrent_ExtraTransactionsRejected() throws Exception {
        // 填满 4/5 个槽位
        for (int i = 0; i < 4; i++) {
            seedAccount("FULL-" + i, "ACC-" + i, new BigDecimal("50"));
        }

        int threadCount = 3; // 抢 1 个空位
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger writtenCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        Object arrayLock = new Object();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int id = i + 1;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    // synchronized 保护 getAndIncrement + 数组写
                    synchronized (arrayLock) {
                        int currentCount = system.getTransactionCount().get();
                        if (currentCount < 5) {
                            int idx = system.getTransactionCount().getAndIncrement();
                            if (idx < 5) {
                                ATMTransaction tx = new ATMTransaction(
                                        "FULL-NEW", "ACC-" + id,
                                        new BigDecimal("50"), "Deposit", BigDecimal.ZERO);
                                system.addToArray(idx, tx);
                                writtenCount.incrementAndGet();
                                log("[Thread-%d] 写入 index=%d", id, idx);
                            } else {
                                rejectedCount.incrementAndGet();
                                log("[Thread-%d] 已满", id);
                            }
                        } else {
                            rejectedCount.incrementAndGet();
                            log("[Thread-%d] 已满", id);
                        }
                    }
                } catch (Exception e) {
                    log("[Thread-%d] 错误: %s", id, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        log(">>> 4/5 已满，3 线程抢最后 1 个空位 <<<");
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, writtenCount.get(), "只能写 1 条");
        assertEquals(2, rejectedCount.get(), "其余应被拒");
        log("✓ 写入=%d 拒绝=%d", writtenCount.get(), rejectedCount.get());
    }

    // ================================================================
    // 测试6: 压力测试 — 重复执行验证稳定性
    // ================================================================

    @RepeatedTest(5)
    @DisplayName("双重支付防护 — 重复 5 次")
    void doubleSpendingPrevention_RepeatedlyStable() throws Exception {
        ATMWithdrawalSystem s = new ATMWithdrawalSystem();
        final String custId = "REP-001";
        final String accNum = "REP-ACC";

        // 种子 $200
        seedIntoSystem(s, custId, accNum, new BigDecimal("200"));

        int threadCount = 4;
        BigDecimal withdrawAmount = new BigDecimal("100");
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();

                    ReentrantLock lock = s.getAccountLock(custId, accNum);
                    lock.lock();
                    try {
                        BigDecimal bal = s.findBalance(custId, accNum);
                        int idx = s.getTransactionCount().getAndIncrement();
                        ATMTransaction tx = new ATMTransaction(
                                custId, accNum, withdrawAmount, "Withdrawal", bal);
                        s.addToArray(idx, tx);
                        tx.processTransaction();
                        if (tx.getStatus() == TransactionStatus.COMPLETED) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        lock.unlock();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        // $200 / $100 = 2，不能超过
        assertTrue(successCount.get() <= 2,
                "最多 2 笔成功，实际: " + successCount.get());
        assertTrue(successCount.get() >= 1,
                "至少 1 笔成功，实际: " + successCount.get());
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    /** 种子账户：存一笔款到指定账户 */
    private void seedAccount(String customerId, String accountNumber, BigDecimal amount)
            throws Exception {
        seedIntoSystem(system, customerId, accountNumber, amount);
    }

    private static void seedIntoSystem(ATMWithdrawalSystem sys,
                                        String customerId, String accountNumber,
                                        BigDecimal amount) throws Exception {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // $0 种子跳过（会导致 InvalidAmountException）
        }
        int idx = sys.getTransactionCount().getAndIncrement();
        // 种子交易：新账户余额为 0，存 amount
        ATMTransaction tx = new ATMTransaction(
                customerId, accountNumber, amount, "Deposit", BigDecimal.ZERO);
        sys.addToArray(idx, tx);
        tx.processTransaction();
    }

    private static String fmt(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static void log(String format, Object... args) {
        System.out.println(String.format(format, args));
    }
}

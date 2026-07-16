package com.abcbank.atm;

import java.util.Scanner;  // Scanner 用于用户输入

/**
 * ATMWithdrawalSystem - 第5组 ATM提款系统 主类
 *
 * ============ PDF 要求对照 ============
 * ✓ Variables          - 成员变量和局部变量
 * ✓ Scanner            - 用户输入
 * ✓ if/else            - 余额判断 (Successful/Rejected)
 * ✓ switch             - 交易类型选择
 * ✓ Loops              - for/while 循环处理交易
 * ✓ Arrays             - 存储 5 条交易记录
 * ✓ Classes            - ATMTransaction 类
 * ✓ Objects            - 创建 ATMTransaction 对象实例
 * ✓ Methods            - 业务方法封装
 * ✓ Organising Classes - 分包、分职责组织代码
 *
 * @author ABC Bank - Java Junior Developer
 * @version 1.0
 */
public class ATMWithdrawalSystem {

    // ============ 成员变量 (Variables) ============
    private static final int MAX_TRANSACTIONS = 5;  // Array of 5 (PDF要求)
    private ATMTransaction[] transactions;           // Array (数组)
    private int transactionCount;                    // 当前交易计数

    // ============ 构造方法 ============
    public ATMWithdrawalSystem() {
        transactions = new ATMTransaction[MAX_TRANSACTIONS]; // 初始化Array
        transactionCount = 0;
    }

    // ============ 主方法 - 程序入口 ============
    public static void main(String[] args) {
        ATMWithdrawalSystem atmSystem = new ATMWithdrawalSystem();  // 创建Object对象
        atmSystem.run();
    }

    // ============ 核心业务方法 (Methods) ============

    /**
     * 运行ATM提款系统主流程 - 使用Loop循环控制
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);  // Scanner 对象
        int choice = 0;

        System.out.println("================================================");
        System.out.println("   ABC Bank ATM Withdrawal System (Group 5)");
        System.out.println("   ABC银行 ATM提款系统 (第5组)");
        System.out.println("================================================");

        // while Loop - 主菜单循环
        while (choice != 6) {
            displayMenu();

            System.out.print("\n请输入您的选择 (Enter your choice): ");
            choice = scanner.nextInt();
            scanner.nextLine(); // 消费换行符

            // switch 语句 - 根据用户选择执行不同操作 (PDF要求)
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
                    System.out.println("\n感谢使用 ABC Bank ATM 系统。再见!");
                    System.out.println("Thank you for using ABC Bank ATM System. Goodbye!");
                    break;
                default:
                    System.out.println("[错误] 无效选择，请重新输入 (Invalid choice)!");
            }
        }

        scanner.close();
    }

    // ============ 功能方法 ============

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
        System.out.println("6. 退出 (Exit)");
        System.out.println("--------------------------------------------------------");
    }

    /**
     * 案例1: 添加交易记录
     * 流程:
     *   1. 输入 Customer ID 和 Account Number
     *   2. 检查账户是否存在 -> 不存在则创建(余额=0), 存在则显示余额
     *   3. 选择交易类型
     *   4. 输入交易金额
     *   5. 处理交易 (if/else 判断 Successful / Rejected)
     */
    private void addTransaction(Scanner scanner) {
        // if/else - 判断Array是否已满
        if (transactionCount >= MAX_TRANSACTIONS) {
            System.out.println("\n[错误] 交易记录已满! 最多存储 " + MAX_TRANSACTIONS + " 条记录。");
            return;
        }

        System.out.println("\n========== 添加第 " + (transactionCount + 1) + " 条交易记录 ==========");

        // ---- 第1步: Scanner 读取客户ID和账户号码 (PDF要求) ----
        System.out.print("客户ID (Customer ID):     ");
        String customerId = scanner.nextLine();

        System.out.print("账户号码 (Account Number): ");
        String accountNumber = scanner.nextLine();

        // ---- 第2步: 使用 if/else + Loop 检查账户是否存在 ----
        double currentBalance = findAccountBalance(customerId, accountNumber);

        // if/else - 判断账户是否已存在
        if (currentBalance < 0) {
            // 账户不存在 -> 创建新账户，余额默认置为 0
            System.out.println("\n[提示] 客户ID " + customerId + " 的账户 " + accountNumber + " 不存在!");
            System.out.println("[系统] 正在为您创建新账户...");
            currentBalance = 0.0;
            System.out.println("[成功] 新账户已创建! 客户ID: " + customerId
                    + ", 账户号码: " + accountNumber + ", 当前余额: $0.00");
        } else {
            // 账户存在 -> 显示当前余额
            System.out.println("\n[提示] 账户已存在! 当前余额: $" + String.format("%.2f", currentBalance));
        }

        // ---- 第3步: switch 选择交易类型 (PDF要求) ----
        String transactionType = selectTransactionType(scanner);

        // ---- 第4步: Scanner 读取交易金额 ----
        System.out.print("交易金额 (Transaction Amount): $");
        double amount = scanner.nextDouble();
        scanner.nextLine(); // 消费换行符

        // ---- 第5步: 创建Object对象并立即处理交易 ----
        ATMTransaction transaction = new ATMTransaction(
                customerId, accountNumber, amount, transactionType, currentBalance);
        transactions[transactionCount] = transaction;
        transactionCount++;

        // 立即处理提款交易，使用 if/else 判断 Successful / Rejected
        System.out.println("\n--- 处理交易结果 ---");
        transaction.processTransaction();

        // if/else - 根据状态打印对应的结果信息
        if (transaction.getStatus().equals("Successful")) {
            System.out.println(">>> 交易成功 (Successful) <<<");
        } else {
            System.out.println(">>> 交易失败 (Rejected) <<<");
        }

        System.out.println("[成功] 交易记录添加成功! (当前共 " + transactionCount + " 条记录)");
    }

    /**
     * 查找账户余额 - 使用 for Loop 在 Array 中搜索匹配的客户ID和账户号码
     *
     * @param customerId    客户ID
     * @param accountNumber 账户号码
     * @return 账户当前余额，如果账户不存在则返回 -1.0
     */
    private double findAccountBalance(String customerId, String accountNumber) {
        double balance = -1.0; // 初始值 -1 表示未找到

        // for Loop - 遍历Array查找已有账户 (PDF要求)
        for (int i = 0; i < transactionCount; i++) {
            ATMTransaction txn = transactions[i];

            // if 条件: 同时匹配客户ID和账户号码
            if (txn.getCustomerId().equals(customerId)
                    && txn.getAccountNumber().equals(accountNumber)) {
                balance = txn.getAccountBalance(); // 获取最新的余额
            }
        }

        return balance; // 返回余额 或 -1(未找到)
    }

    /**
     * switch 语句 - 选择交易类型 (PDF要求)
     */
    private String selectTransactionType(Scanner scanner) {
        System.out.println("\n交易类型 (Transaction Type):");
        System.out.println("  1. Withdrawal (提款)");
        System.out.println("  2. Deposit (存款)");
        System.out.println("  3. Balance Inquiry (余额查询)");
        System.out.println("  4. Transfer (转账)");
        System.out.println("  5. Fast Cash (快速取款)");
        System.out.print("请选择 (1-5): ");

        int typeChoice = scanner.nextInt();
        scanner.nextLine(); // 消费换行符

        String type;
        switch (typeChoice) {
            case 1:
                type = "Withdrawal";
                break;
            case 2:
                type = "Deposit";
                break;
            case 3:
                type = "Balance Inquiry";
                break;
            case 4:
                type = "Transfer";
                break;
            case 5:
                type = "Fast Cash";
                break;
            default:
                System.out.println("[警告] 无效选择，默认设为 Withdrawal");
                type = "Withdrawal";
        }
        return type;
    }

    /**
     * 案例2: 处理所有提款交易
     * 使用 for Loop 遍历Array，使用 if/else 判断成功/拒绝
     */
    private void processAllWithdrawals() {
        // if/else - 判断是否有交易记录
        if (transactionCount == 0) {
            System.out.println("\n[提示] 暂无交易记录，请先添加交易!");
            return;
        }

        System.out.println("\n========== 处理所有提款交易 ==========");

        // for Loop - 遍历Array处理每笔交易 (PDF要求)
        for (int i = 0; i < transactionCount; i++) {
            ATMTransaction txn = transactions[i];
            System.out.println("\n--- 处理交易 #" + (i + 1) + " ---");
            System.out.println("  客户ID: " + txn.getCustomerId());
            System.out.println("  账户:   " + txn.getAccountNumber());
            System.out.println("  类型:   " + txn.getTransactionType());
            System.out.println("  金额:   $" + String.format("%.2f", txn.getTransactionAmount()));

            // if/else - 判断提款是否成功 (PDF核心要求: Successful / Rejected)
            txn.processTransaction();

            // 再次用 if/else 显示最终状态
            if (txn.getStatus().equals("Successful")) {
                System.out.println("  >>> 状态: Successful (成功) <<<");
            } else {
                System.out.println("  >>> 状态: Rejected (拒绝) <<<");
            }
        }

        System.out.println("\n[完成] 所有交易已处理完毕!");
    }

    /**
     * 案例3: 显示所有交易记录
     * 使用 for Loop 遍历显示
     */
    private void displayAllTransactions() {
        if (transactionCount == 0) {
            System.out.println("\n[提示] 暂无交易记录!");
            return;
        }

        System.out.println("\n==================== 所有交易记录 ====================");
        printTableHeader();

        // for Loop 遍历Array (PDF要求)
        for (int i = 0; i < transactionCount; i++) {
            transactions[i].displayTransaction();
        }

        printTableFooter();
        System.out.println("  共 " + transactionCount + " 条记录");
    }

    /**
     * 案例4: 显示汇总报告
     * 使用 Loop 统计 Successful 和 Rejected 数量
     */
    private void displaySummary() {
        if (transactionCount == 0) {
            System.out.println("\n[提示] 暂无交易记录!");
            return;
        }

        int successfulCount = 0;  // Variable
        int rejectedCount = 0;    // Variable
        double totalSuccessfulAmount = 0.0;
        double totalRejectedAmount = 0.0;

        // for Loop 统计 (PDF要求)
        for (int i = 0; i < transactionCount; i++) {
            ATMTransaction txn = transactions[i];

            // if/else 按状态分类统计
            if (txn.getStatus().equals("Successful")) {
                successfulCount++;
                totalSuccessfulAmount += txn.getTransactionAmount();
            } else {
                rejectedCount++;
                totalRejectedAmount += txn.getTransactionAmount();
            }
        }

        System.out.println("\n==================== 汇总报告 (Summary) ====================");
        System.out.println("  总交易数 (Total Transactions):     " + transactionCount);
        System.out.println("  --------------------------------------------------");
        System.out.println("  Successful (成功):                  " + successfulCount
                + "  笔  |  金额合计: $" + String.format("%.2f", totalSuccessfulAmount));
        System.out.println("  Rejected  (拒绝):                   " + rejectedCount
                + "  笔  |  金额合计: $" + String.format("%.2f", totalRejectedAmount));
        System.out.println("  --------------------------------------------------");

        // if/else - 显示成功率
        double successRate = (double) successfulCount / transactionCount * 100;
        System.out.println("  成功率 (Success Rate):              "
                + String.format("%.1f", successRate) + "%");

        if (successRate == 100.0) {
            System.out.println("  评价: 所有交易均成功处理! (All Successful)");
        } else if (successRate >= 50.0) {
            System.out.println("  评价: 大部分交易成功处理。");
        } else {
            System.out.println("  评价: 需要关注 - 拒绝率较高。");
        }
        System.out.println("===========================================================");
    }

    /**
     * 案例5: 按状态搜索
     * 使用 if/else 和 Loop 筛选
     */
    private void searchByStatus(Scanner scanner) {
        if (transactionCount == 0) {
            System.out.println("\n[提示] 暂无交易记录!");
            return;
        }

        System.out.println("\n========== 按状态搜索 ==========");
        System.out.println("  1. Successful (成功)");
        System.out.println("  2. Rejected  (拒绝)");
        System.out.print("请选择状态 (1-2): ");

        int statusChoice = scanner.nextInt();
        scanner.nextLine();

        String searchStatus;
        // if/else 确定搜索条件
        if (statusChoice == 1) {
            searchStatus = "Successful";
        } else {
            searchStatus = "Rejected";
        }

        System.out.println("\n--- 搜索条件: " + searchStatus + " ---");
        printTableHeader();

        boolean found = false;
        // for Loop 遍历搜索
        for (int i = 0; i < transactionCount; i++) {
            // if 条件筛选
            if (transactions[i].getStatus().equals(searchStatus)) {
                transactions[i].displayTransaction();
                found = true;
            }
        }

        if (!found) {
            System.out.println("|               (无匹配记录 / No matching records)              |");
        }

        printTableFooter();
    }

    // ============ 辅助格式化方法 ============

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

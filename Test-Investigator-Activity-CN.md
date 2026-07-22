# 测试侦探——你能解释每一行代码吗？

**时长：** 25–30 分钟

## 目标

通过本次活动，学生应能够解释**为什么 MockMvc 测试中的每一行代码都是这样写的**，而不仅仅是照抄代码。

---

## 活动说明

### 第一步（5 分钟）

将学生分成 2–3 人一组。

给每组发放**完整代码**（即上面的参考答案）。

告诉他们：

> "这段代码是正确的。你们的任务**不是**重写它，而是化身软件侦探，解释每一行重要代码**为什么**是必需的。"

### 第二步（15 分钟）（所有小组）

每组完成下面的工作表。


| 代码                                                           | 用自己的话解释                                | 答案                                                                                                                                                                                                                    |
| -------------------------------------------------------------- | --------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `@WebMvcTest(StockController.class)`                           | 为什么需要这个注解？                          | 它只加载`StockController` 所需的 Spring MVC 网络层——路由、JSON 序列化、校验、异常处理——而不启动完整的应用上下文。不会加载 `@Service`、`@Repository` 或数据库相关的 Bean，因此测试运行速度快，也不需要连接 MySQL。   |
| `@MockBean StockService`                                       | 为什么不使用真实的`StockService`？            | 真实的 service 会调用真实的 repository，而 repository 需要真实的数据库。使用`@MockBean` 会在 Spring 上下文中用 Mockito 模拟对象替换它，这样控制器测试只验证 HTTP 行为，而不涉及业务逻辑或数据持久化。                   |
| `when(stockService.getAllStocks()).thenReturn(List.of(hsbc));` | Mockito 在这里做了什么？                      | 这是在对模拟对象进行"打桩"（stub）："当这个方法被调用时，返回这个固定值，而不是执行真实逻辑。"这样测试就能精确控制控制器接收到的数据。                                                                                  |
| `mockMvc.perform(get("/api/stocks"))`                          | MockMvc 模拟了什么？                          | 它模拟了一个 HTTP 客户端（比如浏览器或 Postman）向`/api/stocks` 发送真实的 `GET` 请求，并让请求经过 Spring 真实的 `DispatcherServlet`/控制器处理流程——但不会启动真实的网络服务器或端口。                              |
| `.andExpect(status().isOk())`                                  | 为什么期望的状态码是 200？                    | 因为被模拟的 service 成功返回了数据（非空列表），所以控制器执行的是正常的"成功"路径，返回`200 OK`。                                                                                                                     |
| `jsonPath("$[0].symbol")`                                      | `$[0]` 是什么意思？                           | `$` 代表 JSON 响应体的根节点。由于响应是一个 JSON **数组**，`$[0]` 表示"数组中的第一个元素"，`.symbol` 则访问该元素上的 `symbol` 字段。                                                                                 |
| `Optional.empty()`                                             | 为什么会返回 404？                            | 控制器代码会检查 service 返回的`Optional` 是否为空；如果为空，就返回 `404 Not Found`，而不是尝试解包一个不存在的值。这表示"没有找到该 ID 对应的股票"。                                                                  |
| `header().exists("Location")`                                  | 为什么 POST 应该返回 Location 响应头？        | 按照 REST 惯例，当`POST` 创建了一个新资源时，响应中应包含指向新创建资源 URL 的 `Location` 响应头（例如 `/api/stocks/1`），这样客户端就能知道去哪里查找/获取该资源。                                                     |
| `thenThrow(new IllegalArgumentException(...))`                 | 为什么用`thenThrow()` 而不是 `thenReturn()`？ | `thenReturn()` 用于打桩一个正常的返回值；`thenThrow()` 则让模拟对象抛出异常。它用于测试错误路径——例如模拟 service 拒绝重复的股票代码——这样测试就能验证控制器/异常处理器是否正确响应（例如返回 `400 Bad Request`）。 |

> **提醒学生：** 不要用搜索引擎查资料，先和组员讨论。

### 第三步（10 分钟）

选择不同小组，每组解释一行代码。

**第 1 组** —— 解释：

```java
@MockBean
private StockService stockService;
```

**参考答案：** Spring 用一个 Mockito 模拟对象替换了真实的 `StockService`。这使我们可以控制 service 返回的内容，而无需调用真实的业务逻辑或数据库。

**第 2 组和第 5 组** —— 解释：

```java
when(stockService.getStockById(99L))
    .thenReturn(Optional.empty());
```

**参考答案：** Mockito 假装 ID 为 99 的股票不存在。因此，控制器应该返回 HTTP 404。

**第 3 组和第 6 组** —— 解释：

```java
mockMvc.perform(get("/api/stocks/99"))
```

**参考答案：** MockMvc 模拟了浏览器向控制器发送 GET 请求，而无需启动真实的网络服务器。

**第 4 组和第 7 组** —— 解释：

```java
.andExpect(status().isNotFound())
```

**参考答案：** 由于 service 返回了 `Optional.empty()`，控制器响应 HTTP 404 Not Found。

---

## 挑战问题（最后 5 分钟）

**1. 为什么有些测试用 `when(...).thenReturn(...)`，而有些用 `thenThrow(...)`？**

`thenReturn(...)` 用于模拟**正常路径（happy path）**——即成功、正常的结果（例如找到了股票、返回了列表）。`thenThrow(...)` 用于模拟**错误/异常路径**——例如重复的股票代码、校验失败或违反业务规则——这样我们就能验证控制器/`@ControllerAdvice` 是否正确地将该异常转换为合适的 HTTP 错误响应（如 `400` 或 `409`）。

**2. 为什么 POST 返回 201 Created，而 GET 返回 200 OK？**

这遵循标准的 HTTP/REST 语义：

- `200 OK` 表示"请求成功，这是现有的资源/数据"（适用于只读取数据的 `GET`）。
- `201 Created` 表示"成功创建了一个新资源"，这就是为什么新增一支股票的 `POST` 会返回 `201`，通常还会附带一个指向新资源的 `Location` 响应头。

**3. 为什么 `jsonPath("$[0].symbol")` 用了 `$[0]`，而 `jsonPath("$.companyName")` 没有？**

这取决于**响应 JSON 的结构**。`GET /api/stocks` 返回的是一个股票的 JSON **数组**，所以必须先用索引（`$[0]`）定位到第一个对象，才能访问字段。而 `GET /api/stocks/1` 返回的是**单个 JSON 对象**，所以 `$` 已经直接指向该对象，`$.companyName` 无需数组索引即可访问字段。

**4. 如果去掉 `@MockBean` 会发生什么？**

没有 `@MockBean`，Spring 就找不到任何 Bean 来满足 `StockController` 所需的 `StockService` 依赖（因为 `@WebMvcTest` 不会扫描 `@Service` Bean）。应用上下文将无法启动，测试会因为 `NoSuchBeanDefinitionException`（依赖未满足）而失败，而不是真正测试到任何业务逻辑。

**5. 为什么我们用 MockMvc 而不是打开浏览器测试？**

`MockMvc` 可以**以编程方式自动地**测试控制器层，耗时仅几毫秒，无需启动真实的 HTTP 服务器或网络端口。它可重复执行、可脚本化，能作为构建流程的一部分（`mvn test`）运行，可在 CI 流水线中无需人工干预地执行，并且能精确断言状态码、响应头和 JSON 响应体内容——这些都是手动在浏览器里点击操作无法做到可靠且实用的。

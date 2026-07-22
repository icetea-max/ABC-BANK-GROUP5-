# Test Investigator – Can You Explain Every Line?

**Duration:** 25–30 minutes

## Objective

By the end of this activity, students should be able to explain **why every line of a
MockMvc test is written**, not just copy it.

---

## Instructions

### Step 1 (5 minutes)

Put students into groups of **2–3**.

Give each group the **complete code** (the answer above).

Tell them:

> "The code is correct. Your task is NOT to rewrite it. Your task is to become software
> investigators and explain WHY every important line is needed."

### Step 2 (15 minutes) (All Groups)

Each group completes the worksheet below.

| Code | Explain in Your Own Words | Answer |
|---|---|---|
| `@WebMvcTest(StockController.class)` | Why is this annotation needed? | It loads only the Spring MVC web layer for `StockController` — routing, JSON serialization, validation, exception handling — without starting the full application context. No `@Service`, `@Repository`, or database beans are loaded, so tests run fast and don't need MySQL. |
| `@MockBean StockService` | Why don't we use the real `StockService`? | The real service would call the real repository, which would need a real database. Using `@MockBean` swaps it for a Mockito mock inside the Spring context, so the controller test only checks HTTP behavior, not business logic or persistence. |
| `when(stockService.getAllStocks()).thenReturn(List.of(hsbc));` | What is Mockito doing here? | It's stubbing the mock: "when this method is called, return this fixed value instead of running real logic." This lets the test control exactly what data the controller receives. |
| `mockMvc.perform(get("/api/stocks"))` | What does MockMvc simulate? | It simulates an HTTP client (like a browser or Postman) sending a real `GET` request to `/api/stocks`, and routes it through Spring's actual `DispatcherServlet`/controller pipeline — but without starting a real network server or port. |
| `.andExpect(status().isOk())` | Why is the expected status 200? | Because the mocked service returned data successfully (a non-empty list), so the controller's normal "success" path executes, which returns `200 OK`. |
| `jsonPath("$[0].symbol")` | What does `$[0]` mean? | `$` is the root of the JSON response body. Since the response is a JSON **array**, `$[0]` means "the first element of that array," and `.symbol` accesses the `symbol` field on that element. |
| `Optional.empty()` | Why does it return 404? | The controller is written to check if the `Optional` returned by the service is empty; if so, it responds with `404 Not Found` instead of trying to unwrap a missing value. It signals "no stock exists with that ID." |
| `header().exists("Location")` | Why should POST return a Location header? | By REST convention, when a `POST` creates a new resource, the response should include a `Location` header pointing to the URL of the newly created resource (e.g., `/api/stocks/1`), so the client knows where to find/fetch it. |
| `thenThrow(new IllegalArgumentException(...))` | Why do we use `thenThrow()` instead of `thenReturn()`? | `thenReturn()` stubs a normal return value; `thenThrow()` stubs the mock to throw an exception instead. It's used when testing error paths — e.g., simulating a service rejecting a duplicate symbol — so the test can verify the controller/exception handler responds correctly (e.g., with `400 Bad Request`). |

> **Reminder to students:** Don't Google. Discuss with your teammates first.

### Step 3 (10 minutes)

Choose different groups to explain one line each.

**Group 1** — Explain:
```java
@MockBean
private StockService stockService;
```
**Expected explanation:** Spring replaces the real `StockService` with a Mockito mock. This allows us to control what the service returns without calling the real business logic or database.

**Group 2 and Group 5** — Explain:
```java
when(stockService.getStockById(99L))
    .thenReturn(Optional.empty());
```
**Expected explanation:** Mockito pretends that no stock with ID 99 exists. Therefore, the controller should return HTTP 404.

**Group 3 and Group 6** — Explain:
```java
mockMvc.perform(get("/api/stocks/99"))
```
**Expected explanation:** MockMvc simulates a browser sending a GET request to the controller without starting a real web server.

**Group 4 and Group 7** — Explain:
```java
.andExpect(status().isNotFound())
```
**Expected explanation:** Since the service returned `Optional.empty()`, the controller responds with HTTP 404 Not Found.

---

## Challenge Question (Last 5 Minutes)

**1. Why do we use `when(...).thenReturn(...)` in some tests but `thenThrow(...)` in others?**

`thenReturn(...)` is used to simulate the **happy path** — a normal, successful result (e.g., a stock is found, a list is returned). `thenThrow(...)` is used to simulate an **error/exceptional path** — e.g., a duplicate symbol, a validation failure, or a business rule violation — so we can verify the controller/`@ControllerAdvice` correctly translates that exception into an appropriate HTTP error response (like `400` or `409`).

**2. Why does POST return 201 Created while GET returns 200 OK?**

These follow standard HTTP/REST semantics:
- `200 OK` means "the request succeeded and here is the existing resource/data" (appropriate for `GET`, which only reads data).
- `201 Created` means "a new resource was successfully created," which is why `POST` — which adds a new stock — returns `201`, typically alongside a `Location` header pointing to the new resource.

**3. Why does `jsonPath("$[0].symbol")` use `$[0]`, but `jsonPath("$.companyName")` does not?**

It depends on the **shape of the JSON response**. `GET /api/stocks` returns a JSON **array** of stocks, so you must index into it (`$[0]`) to reach the first object before accessing a field. `GET /api/stocks/1` returns a **single JSON object**, so `$` already refers to that object directly, and `$.companyName` accesses the field with no array index needed.

**4. What would happen if we removed `@MockBean`?**

Without `@MockBean`, Spring would have no bean to satisfy the `StockService` dependency required by `StockController` (since `@WebMvcTest` doesn't scan `@Service` beans). The application context would fail to start, and the test would fail with a `NoSuchBeanDefinitionException` (unsatisfied dependency) rather than actually testing anything.

**5. Why do we use MockMvc instead of opening a browser?**

`MockMvc` lets you test the controller layer **programmatically and automatically**, in milliseconds, without starting a real HTTP server or network port. It's repeatable, scriptable as part of the build (`mvn test`), works in CI pipelines with no manual interaction, and lets you assert precisely on status codes, headers, and JSON body content — none of which is practical or reliable to do by manually clicking around in a browser.

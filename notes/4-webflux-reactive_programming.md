# Spring WebFlux and Reactive Programming

> **Purpose:** Understand Spring WebFlux, Reactive Programming, Project Reactor, `Mono`, `Flux`, non-blocking I/O, WebClient, R2DBC, and parallel API invocation.

---

# 1. What is Spring WebFlux?

Spring WebFlux is Spring's **reactive, non-blocking web framework**.

It is designed for applications that need to handle:
- High numbers of concurrent requests
- I/O-heavy operations
- Asynchronous processing
- Streaming data

WebFlux is commonly built around **Project Reactor**.

Its two main reactive types are:

- `Mono<T>` – produces 0 or 1 value.
- `Flux<T>` – produces 0 to N values.

# 2. Why was Spring WebFlux introduced?

Traditional Spring MVC generally follows a blocking, thread-per-request model.

### Spring MVC

```text
Client
  |
  v
Tomcat Worker Thread
  |
  v
Database / External API
  |
  | Thread waits for response
  |
  v
Response
```

While waiting for I/O, the worker thread is blocked and cannot process another request.

### Spring WebFlux

```text
Client
  |
  v
Event Loop
  |
  +--> Start I/O operation
  |
  +--> Handle another request
  |
  +--> Handle another request
  |
  +--> Receive I/O completion
  |
  v
Continue processing
```

WebFlux uses non-blocking I/O so that event-loop threads can handle other work while I/O operations are pending.

# 3. Spring MVC vs Spring WebFlux

| Feature           | Spring MVC                         | Spring WebFlux                           |
|-------------------|------------------------------------|------------------------------------------|
| Programming model | Imperative                         | Reactive                                 |
| Common server     | Tomcat                             | Netty                                    |
| Thread model      | Thread-per-request                 | Event-loop based                         |
| I/O               | Usually blocking                   | Supports non-blocking I/O                |
| Return types      | Objects, `ResponseEntity`, etc.    | `Mono`, `Flux`                           |
| Backpressure      | No native Reactive Streams support | Supported                                |
| Streaming         | Supported                          | Natural fit                              |
| Best suited for   | Traditional applications           | High-concurrency, I/O-heavy applications |

# 4. Blocking vs Non-blocking I/O

### Blocking I/O

Blocking I/O means the executing thread waits until an I/O operation completes.

Example: JDBC database query.

```text
Application Thread
       |
       v
Execute JDBC query
       |
       v
Wait for database response
       |
       | Thread is blocked
       |
       v
Database returns result
       |
       v
Continue execution
```

Common blocking operations:
- JDBC database queries
- Blocking HTTP clients
- File reading and writing

### Non-blocking I/O

Non-blocking I/O allows the application to initiate an I/O operation and continue processing other work without synchronously waiting for the result.

```text
Event Loop
    |
    v
Start database/API operation
    |
    +--> Handle Request B
    |
    +--> Handle Request C
    |
    v
Receive I/O completion
    |
    v
Continue Request A
```

**Important:** JDBC is blocking. R2DBC provides a reactive, non-blocking alternative for relational databases.

# 5. Synchronous vs Asynchronous

These concepts are related to, but different from, blocking and non-blocking.

| Concept      | Meaning                                                 |
|--------------|---------------------------------------------------------|
| Synchronous  | Caller waits for the result before continuing           |
| Asynchronous | Caller can continue while the operation completes later |
| Blocking     | Executing thread waits for the operation                |
| Non-blocking | Executing thread does not wait synchronously            |

An asynchronous operation can still use a blocking thread internally. Therefore, asynchronous does not automatically mean non-blocking.

# 6. What is Reactive Programming?

Reactive programming is a programming model based on **data streams and asynchronous processing**.

Instead of processing only one value at a time, we can build pipelines that transform and consume streams of data.

```text
Data Source
    |
    v
Flux<User>
    |
    v
filter()
    |
    v
map()
    |
    v
Subscriber
```

Reactive programming provides:
- Asynchronous data processing
- Reactive operators
- Error handling
- Backpressure
- Stream composition

# 7. What is Reactive Streams?

Reactive Streams is a specification for asynchronous stream processing with non-blocking backpressure.

It defines four important interfaces:

1. `Publisher`
2. `Subscriber`
3. `Subscription`
4. `Processor`

Java also provides related interfaces through `java.util.concurrent.Flow`.

Project Reactor implements the Reactive Streams specification.

# 8. What is Project Reactor?

Project Reactor is the reactive library commonly used with Spring WebFlux.

Its two main types are:

```text
Mono<T>  -> 0 or 1 value

Flux<T>  -> 0 to N values
```

It provides operators such as:

```text
map()
flatMap()
filter()
collectList()
doOnNext()
doOnError()
onErrorResume()
retry()
zip()
merge()
concat()
```

# 9. Mono vs Flux

### Mono

`Mono<T>` represents an asynchronous sequence that produces 0 or 1 value.

Example:

```java
Mono<User> user = userRepository.findById(id);
```

Possible outcomes:

```text
User
Empty
Error
```

### Flux

`Flux<T>` represents an asynchronous sequence that produces 0 to N values.

Example:

```java
Flux<User> users = userRepository.findAll();
```

Possible stream:

```text
User 1
User 2
User 3
User 4
...
```

| Mono          | Flux             |
|---------------|------------------|
| 0 or 1 value  | 0 to N values    |
| Single result | Multiple results |
| `Mono<User>`  | `Flux<User>`     |

# 10. Publisher, Subscriber and Subscription

These are the three main participants in Reactive Streams.

### Publisher
Produces data. Examples: `Mono` and `Flux`.

### Subscriber
Consumes the data produced by the Publisher.

### Subscription
Connects the Publisher and Subscriber and allows the Subscriber to request data.

```text
Subscriber
    |
    | request(10)
    v
Publisher
    |
    v
Produces up to 10 requested elements
```

# 11. What is Backpressure?

Backpressure allows a consumer to control how much data it is ready to receive from a producer.

Consider a fast producer and a slow consumer:

```text
Fast Producer
      |
      | Produces too much data
      v
Slow Consumer
```

The consumer may become overwhelmed if data arrives faster than it can process it.

With backpressure:

```text
Consumer
    |
    | request(100)
    v
Producer
    |
    v
Emits requested data
```

Backpressure helps control data flow and reduce resource pressure.

# 12. Lazy Execution and subscribe()

Reactive pipelines are generally **lazy**. Creating a pipeline does not necessarily execute it.

Example:

```java
Flux<Integer> flux =
        Flux.just(1, 2, 3)
            .map(i -> i * 2);
```

At this point, the pipeline has been created, but it has not been subscribed to.

```text
Create Pipeline
      |
      v
No subscription yet
      |
      v
subscribe()
      |
      v
Pipeline starts executing
```

### What does subscribe() do?

`subscribe()` starts consumption of a reactive pipeline.

```
Flux.just(1, 2, 3)
    .map(i -> i * 2)
    .subscribe(System.out::println);
```

Output:

```text
2
4
6
```

**Important:** In normal Spring WebFlux controllers, do not manually call `subscribe()`. Return the `Mono` or `Flux`; Spring WebFlux manages the subscription.

# 13. WebFlux Controller Example

```java
@GetMapping("/users")
public Flux<User> getUsers() {
    return userRepository.findAll();
}
```

Execution flow:

```text
Client
  |
  v
WebFlux
  |
  v
Controller
  |
  v
Flux<User>
  |
  v
WebFlux subscribes
  |
  v
Data is emitted
  |
  v
HTTP Response
```

WebFlux manages subscribing to the returned publisher and writing the response.

# 14. Netty and Event Loop

Spring WebFlux supports multiple server runtimes. **Netty is a commonly used runtime.**

Netty uses an event-loop architecture.

```text
             Netty
               |
       +-------+-------+
       |               |
       v               v
   Event Loop 1     Event Loop 2
       |               |
       v               v
   Connections      Connections
```

An event loop processes events such as:
- Incoming HTTP requests
- Socket readiness
- I/O completion
- Network writes

A small number of event-loop threads can handle many concurrent connections when operations are non-blocking.

### Why should event-loop threads not block?

Suppose an event-loop thread executes a blocking JDBC query.

```text
Event Loop Thread
       |
       v
Blocking JDBC query
       |
       v
Waiting for DB
       |
       v
Other events cannot be processed efficiently
```

This can affect multiple requests handled by that event-loop thread.

**Rule:** Never execute blocking operations directly on WebFlux event-loop threads.

# 15. CPU-bound vs I/O-bound Operations

### CPU-bound

The application spends most of its time performing calculations.

Examples:
- Sorting large datasets
- Encryption
- Image processing
- Complex calculations

### I/O-bound

The application spends much of its time waiting for external resources.

Examples:
- Database queries
- REST API calls
- File operations
- Redis operations
- Kafka communication

WebFlux is particularly useful for high-concurrency I/O-bound workloads.

# 16. Reactive Operators

Operators transform, filter, combine or handle values in reactive streams.

### map()

Transforms one value into another.

```
Flux.just(1, 2, 3)
    .map(i -> i * 10);
```

Output:

```text
10
20
30
```

One input produces one output.

### flatMap()

Used when each input produces another Publisher.

```java
Flux<Integer> ids = Flux.just(1, 2, 3);

Flux<User> users =
        ids.flatMap(id -> userService.getUser(id));
```

If `getUser()` returns `Mono<User>`, `flatMap()` combines the resulting publishers into a `Flux<User>`.

### map() vs flatMap()

```java
// map: transforms a value
Mono<String> name =
        userService.findUser(id)
                   .map(User::getName);

// flatMap: composes another Publisher
Mono<Address> address =
        userService.findUser(id)
                   .flatMap(user ->
                       addressService.getAddress(user.getId()));
```

| map()                              | flatMap()                           |
|------------------------------------|-------------------------------------|
| Transforms a value                 | Transforms and flattens a Publisher |
| One-to-one transformation          | Useful for asynchronous composition |
| Does not flatten nested publishers | Avoids nested `Mono<Mono<T>>`       |

### filter()

Emits only values that satisfy a condition.

```
Flux.just(1, 2, 3, 4)
    .filter(i -> i % 2 == 0);
```

Output:

```text
2
4
```

### collectList()

Converts `Flux<T>` into `Mono<List<T>>`.

```java
Mono<List<User>> users =
        userRepository.findAll().collectList();
```

```text
Flux<User>
    |
    v
collectList()
    |
    v
Mono<List<User>>
```

### doOnNext()

Used for side effects such as logging. It does not transform the value.

```
Flux.just(1, 2, 3)
    .doOnNext(i -> log.info("Value: {}", i))
    .map(i -> i * 2);
```

# 17. Reactive Error Handling

Common error-handling operators:

| Operator          | Purpose                     |
|-------------------|-----------------------------|
| `doOnError()`     | Observe or log an error     |
| `onErrorReturn()` | Return a fallback value     |
| `onErrorResume()` | Switch to another Publisher |
| `onErrorMap()`    | Transform an error          |
| `retry()`         | Resubscribe after an error  |

### onErrorResume()

Provides an alternative Publisher when an error occurs.

```java
Mono<User> result =
        userService.findUser(id)
            .onErrorResume(ex ->
                userService.getDefaultUser()
            );
```

Flow:

```text
findUser()
    |
    +---- Success ---> User
    |
    +---- Error -----> getDefaultUser()
```

### retry()

Retries a failed Publisher by resubscribing.

```
webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class)
    .retry(3);
```

Retry must be used carefully, especially for non-idempotent operations.

# 18. Combining Reactive Streams

### zip()

Combines values from multiple publishers based on corresponding positions.

```java
Mono<User> user = userService.getUser(id);
Mono<Address> address = addressService.getAddress(id);

Mono<Tuple2<User, Address>> result =
        Mono.zip(user, address);
```

Useful when independent operations need to complete before continuing.

### merge()

Combines publishers and emits values as they arrive.

```java
Flux<Integer> result = Flux.merge(flux1, flux2);
```

The output order depends on emission timing.

### concat()

Subscribes to publishers sequentially.

```java
Flux<Integer> result = Flux.concat(flux1, flux2);
```

```text
Flux 1
   |
   | Completes
   v
Flux 2
```

Unlike `merge()`, `concat()` preserves the sequence of publishers.

# 19. Hot vs Cold Publishers

### Cold Publisher

Each subscriber gets its own execution of the sequence.

```text
Cold Publisher
    |
    +---- Subscriber A --> Full sequence
    |
    +---- Subscriber B --> Full sequence
```

Example: `Flux.just(1, 2, 3)`.

### Hot Publisher

A hot publisher can produce data independently of individual subscribers.

Subscribers may receive only the values emitted after they subscribe.

```text
       Hot Publisher
        /         \
       v           v
 Subscriber A   Subscriber B
```

# 20. WebClient

`WebClient` is Spring's reactive HTTP client.

It supports non-blocking HTTP communication and integrates with Reactor.

Example:

```java
Mono<User> user =
    webClient.get()
        .uri("/users/1")
        .retrieve()
        .bodyToMono(User.class);
```

Request flow:

```text
WebFlux Controller
       |
       v
WebClient
       |
       v
Non-blocking HTTP request
       |
       v
External Service
       |
       v
Response
       |
       v
Mono<User>
```

### WebClient vs RestTemplate

| Feature              | RestTemplate                   | WebClient                         |
|----------------------|--------------------------------|-----------------------------------|
| API model            | Blocking                       | Reactive                          |
| Return type          | Direct object                  | `Mono` / `Flux`                   |
| Non-blocking I/O     | No                             | Yes, with its non-blocking client |
| Reactive integration | No                             | Yes                               |
| Typical usage        | Existing blocking applications | Reactive applications             |

For new reactive applications, prefer `WebClient`. `RestTemplate` is mainly relevant to existing blocking applications.

# 21. Parallel API Invocation Using WebClient

Suppose our application needs to create a dashboard.

The dashboard requires:

1. User information
2. Account information
3. Transaction information

All three APIs are independent.

```text
Client
  |
  v
GET /api/dashboard/101
  |
  v
DashboardController
  |
  v
DashboardService
  |
  +----> User API
  |
  +----> Account API
  |
  +----> Transaction API
  |
  v
Combine responses
  |
  v
DashboardResponse
```

Because these APIs are independent, they can be invoked concurrently.

## 21.1 Controller

```java
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{userId}")
    public Mono<DashboardResponse> getDashboard(@PathVariable Long userId) {
        return dashboardService.getDashboard(userId);
    }
}
```

The controller returns `Mono<DashboardResponse>`.

It does not block and wait for the `DashboardResponse`. It returns the reactive pipeline to WebFlux.

## 21.2 Service

```java
@Service
public class DashboardService {
    private final WebClient webClient;
    public DashboardService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<DashboardResponse> getDashboard(Long userId) {
        Mono<UserResponse> userMono = getUser(userId);
        Mono<AccountResponse> accountMono = getAccount(userId);
        Mono<TransactionResponse> transactionMono = getTransactions(userId);
        return Mono.zip(userMono, accountMono, transactionMono).
                map(tuple -> {
                    UserResponse user = tuple.getT1();
                    AccountResponse account = tuple.getT2();
                    TransactionResponse transactions = tuple.getT3();
                    
            return new DashboardResponse(user, account, transactions);
        });
    }

    private Mono<UserResponse> getUser(Long userId) {
        return webClient.get()
                .uri("http://user-service/users/{id}", userId)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    private Mono<AccountResponse> getAccount(Long userId) {
        return webClient.get()
                .uri("http://account-service/accounts/{id}", userId)
                .retrieve()
                .bodyToMono(AccountResponse.class);
    }

    private Mono<TransactionResponse> getTransactions(Long userId) {
        return webClient.get()
                .uri("http://transaction-service/transactions/{id}", userId)
                .retrieve()
                .bodyToMono(TransactionResponse.class);
    }
}
```

## 21.3 Response Classes

### UserResponse

```java
public record UserResponse(Long id, String name) { }
```

### AccountResponse

```java
public record AccountResponse(Long accountId, Double balance) { }
```

### TransactionResponse

```java
public record TransactionResponse(Long transactionId, String type) { }
```

### DashboardResponse

```java
public record DashboardResponse(UserResponse user, AccountResponse account, TransactionResponse transactions) { }
```

## 21.4 What is Mono.zip()?

`Mono.zip()` is a Reactor operator used to combine multiple `Mono` publishers.

```
Mono.zip(userMono, accountMono, transactionMono);
```

Its purpose is:

> Subscribe to the source Mono's when the zipped Mono is subscribed to, wait for their values, and combine them into one result.

Conceptually:

```text
                 +---- User API -----------> User
                 |
Mono.zip() <-----+---- Account API --------> Account
                 |
                 +---- Transaction API ----> Transactions
                                              |
                                              v
                                       Combine results
                                              |
                                              v
                                       DashboardResponse
```

Use `Mono.zip()` when:
- Multiple operations are independent.
- They can execute concurrently.
- All their results are required for the final result.

## 21.5 Sequential vs Parallel API Invocation

Assume the API response times are:

```text
User API          = 500 ms
Account API       = 700 ms
Transaction API   = 600 ms
```

### Sequential invocation

```text
User API
   |
   | 500 ms
   v
Account API
   |
   | 700 ms
   v
Transaction API
   |
   | 600 ms
   v
Response
```

Total:

```text
500 + 700 + 600 = 1800 ms
```

### Concurrent invocation

With `Mono.zip()`, the independent WebClient operations are subscribed to as part of the same combined pipeline.

```text
User API
   |---------------- 500 ms ----------------|

Account API
   |-------------------- 700 ms --------------------|

Transaction API
   |----------------- 600 ms -----------------|

                                               |
                                               v
                                         All completed
                                               |
                                               v
                                            Response
```

Approximate total time:

```text
max(500, 700, 600) = 700 ms
```

This is an illustrative estimate. Actual response time also depends on network latency, service processing, connection pools, and other overhead.

## 21.6 Does Mono.zip() Create Three Threads?

No.

Do not think:

```text
Mono.zip()
   |
   +---- Thread 1
   +---- Thread 2
   +---- Thread 3
```

`Mono.zip()` is primarily a **reactive composition operator**.

It combines multiple publishers and coordinates their results. It does not automatically create one thread per API.

With non-blocking WebClient calls, the HTTP operations can be in flight concurrently without requiring one dedicated thread per request.

## 21.7 What Does It Mean to Subscribe to a Mono?

Consider:

```java
Mono<UserResponse> userMono = getUser(userId);
```

At this point, we have created a reactive pipeline.

```text
Mono<UserResponse>
       |
       v
Reactive Pipeline Created
       |
       v
Not subscribed yet
```

Think of it as:

> When somebody subscribes, execute this pipeline and eventually produce a `UserResponse`.

Subscription means starting consumption of the publisher.

```text
Mono Pipeline
     |
     | subscribe()
     v
Pipeline starts
     |
     v
HTTP request
     |
     v
HTTP response
     |
     v
Mono emits value
```

## 21.8 Does Mono.zip() Subscribe Immediately?

Consider:

```java
Mono<DashboardResponse> dashboardMono =
        Mono.zip(
                userMono,
                accountMono,
                transactionMono
        ).map(tuple -> new DashboardResponse(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3()
        ));
```

`Mono.zip()` does not immediately subscribe to the source Mono's just because this line executes.

Instead, it creates a combined reactive pipeline.

```text
userMono --------\
                  \
accountMono -------> Mono.zip() ---> Combined Mono
                  /
transactionMono -/

          |
          v
   Pipeline created
          |
          v
   Not subscribed yet
```

When the combined Mono is subscribed to, it subscribes to its source Mono's.

```text
Combined Mono
      |
      | subscribe()
      v
  Mono.zip()
      |
      +----> Subscribe to userMono
      |
      +----> Subscribe to accountMono
      |
      +----> Subscribe to transactionMono
      |
      v
  Coordinate results
      |
      v
 DashboardResponse
```

## 21.9 Who Calls subscribe() in WebFlux?

In a normal Spring WebFlux application, **you usually do not call `subscribe()` manually**.

For example:

```java
@GetMapping("/{userId}")
public Mono<DashboardResponse> getDashboard(
        @PathVariable Long userId) {

    return dashboardService.getDashboard(userId);
}
```

The controller returns:

```text
Mono<DashboardResponse>
```

WebFlux manages the reactive lifecycle and subscribes to the returned publisher as part of handling the HTTP request.

Conceptually:

```text
Client
  |
  v
Controller
  |
  v
Service
  |
  v
Mono<DashboardResponse>
  |
  | Return
  v
WebFlux
  |
  | Subscribe
  v
Reactive pipeline starts
  |
  +----> User API
  |
  +----> Account API
  |
  +----> Transaction API
  |
  v
Combine results
  |
  v
DashboardResponse
  |
  v
HTTP Response
```

## 21.10 Why Should We Avoid Manual subscribe()?

Normally, do not write:

```
userMono.subscribe();
accountMono.subscribe();
transactionMono.subscribe();
```

Also avoid:

```
Mono.zip(userMono, accountMono, transactionMono)
    .subscribe();
```

inside your service.

Instead, compose and return the reactive pipeline:

```
return Mono.zip(userMono, accountMono, transactionMono);
```

This allows WebFlux to manage subscription, errors, cancellation, and response writing as part of the request lifecycle.

## 21.11 What Happens If One API Fails?

By default, `Mono.zip()` requires all source Mono's to emit a value successfully.

If one source emits an error, the combined Mono also emits an error and other sources may be cancelled.

If one source completes empty, the combined Mono completes without producing a combined value.

Example:

```text
User API         ---> Success
Account API      ---> Error
Transaction API  ---> Success

                 |
                 v
              Mono.zip()
                 |
                 v
             Error signal
```

If a dashboard should still be returned when one API fails, handle that apis error separately with an appropriate fallback, such as `onErrorResume()`.

---

# 22. R2DBC — Reactive Database Access

R2DBC stands for **Reactive Relational Database Connectivity**.

It provides a reactive API for relational database operations.

### JDBC

```text
Spring MVC
    |
JPA / Hibernate
    |
JDBC
    |
Blocking database call
```

### R2DBC

```text
Spring WebFlux
    |
Reactive Repository
    |
R2DBC Driver
    |
Non-blocking database communication
```

Examples of databases with R2DBC drivers include PostgreSQL, MySQL and Microsoft SQL Server.

### WebFlux + JPA problem

JPA/Hibernate normally uses blocking JDBC.

```text
WebFlux
   |
JPA
   |
JDBC
   |
Blocking I/O
```

Calling JDBC directly on an event-loop thread blocks that thread and can harm scalability.

If blocking JPA/JDBC operations are unavoidable, isolate them on an appropriate scheduler such as `boundedElastic()`.

# 23. Schedulers in Project Reactor

Schedulers control where reactive tasks execute.

Important schedulers:

```text
Schedulers.parallel()
Schedulers.boundedElastic()
Schedulers.single()
Schedulers.immediate()
```

### parallel()

Designed primarily for CPU-bound work.

```
.publishOn(Schedulers.parallel())
```

Uses a pool sized around the available CPU cores.

### boundedElastic()

Designed for potentially blocking work.

```
Mono.fromCallable(() -> blockingOperation())
    .subscribeOn(Schedulers.boundedElastic());
```

Conceptually:

```text
Event Loop
    |
    | Schedule blocking operation
    v
boundedElastic Scheduler
    |
    v
Blocking operation executes
```

This isolates blocking work from event-loop threads.

**Important:** Moving blocking code to another scheduler does not make the operation non-blocking.

### subscribeOn() vs publishOn()

| subscribeOn()                                                     | publishOn()                                         |
|-------------------------------------------------------------------|-----------------------------------------------------|
| Influences where subscription and upstream source execution begin | Switches execution context for downstream operators |
| Typically used near the source                                    | Affects operators after it                          |

Example:

```
Mono.fromCallable(() -> blockingOperation())
    .subscribeOn(Schedulers.boundedElastic())
    .publishOn(Schedulers.parallel())
    .map(result -> process(result));
```

# 24. Functional WebFlux Endpoints

WebFlux supports two programming styles.

### Annotation-based

```java
@RestController
class UserController {

    @GetMapping("/users")
    public Flux<User> getUsers() {
        return userService.findAll();
    }
}
```

### Functional

Functional endpoints use:
- `RouterFunction`
- `HandlerFunction`
- `ServerRequest`
- `ServerResponse`

Example:

```java
RouterFunction<ServerResponse> routes =
    RouterFunctions.route(
        RequestPredicates.GET("/users"),
        request ->
            ServerResponse.ok()
                .body(userService.findAll(), User.class)
    );
```

# 25. Server-Sent Events (SSE)

SSE allows a server to continuously send events to a client over an HTTP connection.

Example:

```java
@GetMapping(
    value = "/events",
    produces = MediaType.TEXT_EVENT_STREAM_VALUE
)
public Flux<String> events() {
    return Flux.interval(Duration.ofSeconds(1))
               .map(i -> "Event " + i);
}
```

Flow:

```text
Server
   |
   +---- Event 1 ----> Client
   |
   +---- Event 2 ----> Client
   |
   +---- Event 3 ----> Client
   |
   +---- Event 4 ----> Client
```

SSE is one-way communication: server to client.

For bidirectional communication, technologies such as WebSocket and RSocket can be used.

# 26. When Should WebFlux Be Used?

WebFlux is a good choice when:
- The application needs high concurrency.
- Most operations are I/O-bound.
- External HTTP calls use non-blocking clients.
- Database access uses reactive drivers.
- Streaming or SSE is required.
- Reactive processing provides a clear benefit.

# 27. When Should Spring MVC Be Used?

Spring MVC is often a better choice when:
- The application is primarily CRUD-based.
- JPA/Hibernate and JDBC are heavily used.
- Existing libraries are blocking.
- The application has moderate concurrency.
- The team prefers imperative programming.
- Reactive programming adds unnecessary complexity.

# 28. Spring MVC + Virtual Threads vs WebFlux

These are different approaches to handling concurrency.

### Traditional Spring MVC

```text
Request
   |
Platform Thread
   |
Blocking I/O
   |
Platform Thread waits
   |
Response
```

### Spring MVC + Virtual Threads

```text
Request
   |
Virtual Thread
   |
Blocking I/O
   |
Virtual Thread parks
   |
Carrier thread becomes available
   |
I/O completes
   |
Virtual Thread resumes
```

### Spring WebFlux

```text
Request
   |
Event Loop
   |
Start non-blocking I/O
   |
Event Loop handles other requests
   |
I/O completion
   |
Reactive pipeline continues
   |
Response
```

| Spring MVC + Virtual Threads | Spring WebFlux                           |
|------------------------------|------------------------------------------|
| Blocking programming model   | Reactive programming model               |
| Virtual threads              | Event-loop based execution               |
| Blocking I/O is supported    | Designed for non-blocking I/O            |
| Imperative code              | Reactive code                            |
| JDBC can be used             | R2DBC is suitable for reactive DB access |

**Remember:**
- Virtual threads make blocking code more scalable.
- WebFlux aims to avoid blocking I/O.
- Neither approach automatically makes an application faster.

# 29. Does WebFlux Always Make Applications Faster?

No. WebFlux is not automatically faster than Spring MVC.

It can improve concurrency and resource utilization when the application uses non-blocking I/O effectively.

However, it also introduces:
- Reactive programming complexity
- More complicated debugging
- Reactive library compatibility requirements

For traditional CRUD applications using JPA/JDBC, Spring MVC is often simpler and more suitable.

---

# 30. Complete WebFlux Architecture

```text
                         CLIENT
                           |
                           v
                    HTTP Request
                           |
                           v
                 Reactive HTTP Server
                     (e.g. Netty)
                           |
                           v
                      Event Loop
                           |
                           v
                  Spring WebFlux
                           |
                           v
                     Controller
                           |
                    Mono / Flux
                           |
             +-------------+-------------+
             |                           |
             v                           v
         WebClient                    R2DBC
             |                           |
             v                           v
     External Service              Reactive Database
             |                           |
             +-------------+-------------+
                           |
                           v
                    Reactive Pipeline
                           |
                 map / flatMap / filter
                           |
                           v
                    Mono / Flux
                           |
                           v
                  HTTP Response
                           |
                           v
                         CLIENT
```

# 31. Quick Revision

### Spring MVC

```text
Request
   |
Tomcat Worker Thread
   |
Blocking I/O
   |
Thread waits
   |
Response
```

### Spring MVC + Virtual Threads

```text
Request
   |
Virtual Thread
   |
Blocking I/O
   |
Virtual Thread parks
   |
Carrier thread becomes available
   |
I/O completes
   |
Virtual Thread resumes
```

### Spring WebFlux

```text
Request
   |
Event Loop
   |
Start non-blocking I/O
   |
Event Loop handles other requests
   |
I/O completion
   |
Reactive pipeline continues
   |
Response
```

# 32. Most Important Interview Points

1. WebFlux is Spring's reactive web framework.
2. Project Reactor is its commonly used reactive library.
3. `Mono` represents 0 or 1 value.
4. `Flux` represents 0 to N values.
5. Netty is a commonly used WebFlux server runtime.
6. WebFlux commonly uses an event-loop architecture.
7. Non-blocking I/O allows threads to handle other work while I/O is pending.
8. Reactive Streams defines Publisher, Subscriber, Subscription and Processor.
9. Backpressure allows consumers to control demand.
10. Reactive pipelines are generally lazy.
11. `subscribe()` starts consumption.
12. Spring WebFlux manages subscriptions for returned controller publishers.
13. `map()` transforms values.
14. `flatMap()` composes and flattens Publishers.
15. `filter()` filters values.
16. `collectList()` converts `Flux<T>` into `Mono<List<T>>`.
17. `onErrorResume()` provides fallback reactive processing.
18. `retry()` resubscribes after an error.
19. `zip()` combines corresponding values from multiple publishers.
20. `merge()` emits values as they arrive.
21. `concat()` processes publishers sequentially.
22. WebClient is Spring's reactive HTTP client.
23. R2DBC provides reactive relational database access.
24. JDBC is blocking; avoid calling it directly on event-loop threads.
25. `boundedElastic()` can isolate unavoidable blocking operations.
26. `parallel()` is primarily intended for CPU-bound work.
27. `subscribeOn()` influences upstream execution.
28. `publishOn()` changes downstream execution context.
29. WebFlux is especially useful for high-concurrency I/O-heavy workloads.
30. WebFlux is not automatically faster than Spring MVC.
31. Virtual threads make blocking code more scalable.
32. WebFlux aims to use non-blocking I/O.
33. SSE supports server-to-client streaming.
34. WebFlux supports annotation-based and functional endpoints.
35. `Mono.zip()` does not create one thread per API.
36. `Mono.zip()` subscribes to its source publishers when the combined publisher is subscribed to.
37. Independent WebClient calls can execute concurrently.
38. In a WebFlux controller, return the reactive pipeline instead of manually calling `subscribe()`.

# 33. Recommended Learning Order

```text
1. Blocking vs Non-blocking I/O
          |
          v
2. Synchronous vs Asynchronous
          |
          v
3. Reactive Programming
          |
          v
4. Reactive Streams
          |
          v
5. Project Reactor
          |
          v
6. Mono and Flux
          |
          v
7. Lazy Execution and subscribe()
          |
          v
8. Reactive Operators
          |
          +--> map()
          +--> flatMap()
          +--> filter()
          +--> zip()
          +--> merge()
          +--> concat()
          |
          v
9. Error Handling
          |
          v
10. Backpressure
          |
          v
11. Schedulers
          |
          v
12. Event Loop and Netty
          |
          v
13. WebFlux Controllers
          |
          v
14. WebClient
          |
          v
15. Parallel API Invocation using Mono.zip()
          |
          v
16. R2DBC
          |
          v
17. SSE and Streaming
          |
          v
18. Functional Endpoints
          |
          v
19. WebFlux vs Spring MVC
          |
          v
20. WebFlux vs Virtual Threads
```

# Final Mental Model

### SPRING MVC

```text
Request
   |
Worker Thread
   |
Blocking I/O
   |
Thread waits
   |
Response
```

### VIRTUAL THREADS

```text
Request
   |
Virtual Thread
   |
Blocking I/O
   |
Virtual Thread parks
   |
Carrier thread can execute other work
   |
I/O completes
   |
Virtual Thread resumes
```

### SPRING WEBFLUX

```text
Request
   |
Event Loop
   |
Non-blocking I/O
   |
Event Loop handles other requests
   |
I/O completion
   |
Reactive pipeline continues
   |
Response
```

**Remember these three differences:**

- Spring MVC + blocking I/O: the worker thread waits.
- Spring MVC + virtual threads: the virtual thread can park while waiting, freeing its carrier thread.
- Spring WebFlux + non-blocking I/O: the event-loop thread can handle other work while I/O is pending.

### Core WebFlux Stack

```text
Spring WebFlux
      |
      v
Project Reactor
      |
      +---- Mono
      |
      +---- Flux
      |
      +---- Operators
      |
      +---- Backpressure
      |
      +---- Schedulers
      |
      v
Reactive Streams
      |
      v
Non-blocking I/O
      |
      +---- WebClient
      |
      +---- R2DBC
      |
      +---- Streaming
```

### Core Parallel API Invocation Pattern

```
Mono<A> aMono = callA();
Mono<B> bMono = callB();
Mono<C> cMono = callC();
return Mono.zip(aMono, bMono, cMono)
        .map(tuple -> {
            A a = tuple.getT1();
            B b = tuple.getT2();
            C c = tuple.getT3();
            return combine(a, b, c);
        });
```

**Final takeaway:**

- `Mono` and `Flux` represent asynchronous data pipelines.
- `WebClient` performs non-blocking HTTP communication.
- `Mono.zip()` combines independent reactive operations.
- Subscription starts the reactive execution.
- Spring WebFlux manages subscription for returned controller publishers.
- WebFlux achieves efficient concurrency through non-blocking I/O, not by creating a separate thread for every request.
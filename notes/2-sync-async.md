# Synchronous, Asynchronous, Blocking and Non-Blocking

## 1. Core Difference

These are two different concepts:

| Concept          | Meaning                                                     |
|------------------|-------------------------------------------------------------|
| **Synchronous**  | Caller waits for the result before continuing.              |
| **Asynchronous** | Caller can continue without waiting for the result.         |
| **Blocking**     | Current thread waits for the operation to complete.         |
| **Non-Blocking** | Current thread does not wait for the operation to complete. |

```text
Synchronous vs Asynchronous
        ↓
    CALLER BEHAVIOR
    Does the caller wait?

Blocking vs Non-Blocking
        ↓
    THREAD BEHAVIOR
    Does the current thread wait?
```

## 2. Synchronous

The caller waits for the operation to complete before continuing.

```
String result = callDatabase();
System.out.println(result);
```

```text
Start
  ↓
Call Database
  ↓
Wait for Result
  ↓
Receive Result
  ↓
Continue Execution
```

**Important:** Synchronous does not mean one request at a time. Multiple threads can execute synchronous requests concurrently.

```text
Request A → Thread 1 → DB → WAIT
Request B → Thread 2 → DB → WAIT
Request C → Thread 3 → DB → WAIT
```

## 3. Asynchronous

The caller starts an operation and can continue doing other work without waiting for its result.

```
CompletableFuture<String> future = callDatabaseAsync();

doOtherWork();

future.thenAccept(result -> {
    System.out.println(result);
});
```

```text
Start Database Operation
          ↓
    Don't Wait Here
          ↓
    Do Other Work
          ↓
    Result Available
          ↓
    Process Result
```

**Important:** Asynchronous does not necessarily mean multiple threads. It can use event loops, callbacks, futures or thread pools.

## 4. Blocking

The current thread waits until the operation completes.

```java
String result = database.call();
```

```text
Thread
  ↓
Database Call
  ↓
Thread WAIT
  ↓
Database Response
  ↓
Continue Execution
```

## 5. Non-Blocking

The current thread does not need to wait for the operation to complete. It can perform other work.

```text
Thread
  ↓
Start I/O Operation
  ↓
Don't Wait
  ↓
Handle Other Work
  ↓
I/O Completes
  ↓
Process Result
```

## 6. Possible Combinations

Synchronous/Asynchronous and Blocking/Non-Blocking are independent concepts.

|                  | Blocking | Non-Blocking |
|------------------|----------|--------------|
| **Synchronous**  | Possible | Possible     |
| **Asynchronous** | Possible | Possible     |

Do not memorize:

```text
Synchronous = Blocking
Asynchronous = Non-Blocking
```

They are not synonyms.

## 7. Spring MVC vs Virtual Threads vs WebFlux

### A. Traditional Spring MVC

**Synchronous + Blocking + Platform Threads**

```text
Request
   ↓
Tomcat Worker Thread
   ↓
Controller → Service → Repository
   ↓
Database Call
   ↓
Thread WAIT
   ↓
Database Response
   ↓
Return Response
```

Each request occupies a worker thread while waiting for blocking I/O.

### B. Spring MVC + Virtual Threads

**Synchronous + Blocking-style Code + Virtual Threads**

```text
Request A → Virtual Thread A → DB → Park
Request B → Virtual Thread B → DB → Park
Request C → Virtual Thread C → DB → Park
```

- Code remains simple and synchronous.
- Blocking I/O can park the virtual thread.
- Its carrier thread becomes available for other work.
- Virtual threads make I/O-heavy applications more scalable.

**But how does a blocking database call work with virtual threads?**

Consider this JDBC-style code:

```java
public User getUser() {

    User user = userRepository.findById(1L);

    System.out.println(user);

    return user;
}
```

Execution flow:

```text
Virtual Thread-1
       |
       v
  Database Call
       |
       v
  Send DB Query
       |
       v
  Wait for DB Response
       |
       v
 Virtual Thread-1 gets parked
       |
       v
 JVM unmounts it from Carrier Thread-1
       |
       v
 Carrier Thread-1 becomes available
       |
       v
 Executes another Virtual Thread
       |
       v
 DB Response Arrives
       |
       v
 Virtual Thread-1 becomes runnable
       |
       v
 Mounted on an available carrier
       |
       v
 Continues execution
```

**Then why is JDBC still called blocking?**

Because the calling virtual thread still waits for the database result before executing the next statement.

The difference is:

- The **virtual thread** waits for the database response.
- The **carrier thread** does not need to wait; it can execute another virtual thread.
- When the response arrives, the original virtual thread resumes execution.

Therefore:

**Virtual threads do not convert blocking APIs like JDBC into non-blocking APIs. They make blocking-style programming more scalable by freeing carrier threads during supported blocking waits.**

### C. Spring WebFlux

**Asynchronous + Non-Blocking + Reactive Model**

```text
Request
   ↓
Reactor Event Loop
   ↓
Start Non-Blocking I/O
   ↓
Handle Other Requests
   ↓
I/O Completes
   ↓
Process Result
   ↓
Return Response
```

The event-loop thread does not sit idle waiting for I/O. It can handle other available work.

## 8. Final Comparison

| Feature           | Spring MVC                  | MVC + Virtual Threads          | Spring WebFlux                       |
|-------------------|-----------------------------|--------------------------------|--------------------------------------|
| Programming Model | Synchronous                 | Synchronous                    | Reactive                             |
| I/O Style         | Blocking                    | Blocking-style                 | Non-Blocking                         |
| Thread Model      | Platform Threads            | Virtual Threads                | Event Loop                           |
| Waiting           | Worker thread waits         | Virtual thread parks           | Event-loop thread handles other work |
| Scalability       | Limited by thread resources | Better for I/O-heavy workloads | Efficient for I/O-heavy workloads    |

## 9. Final Takeaways

1. **Synchronous:** I need the result before continuing.
2. **Asynchronous:** I can continue and handle the result later.
3. **Blocking:** My current thread has to wait.
4. **Non-Blocking:** My current thread does not have to wait.
5. Synchronous requests can execute concurrently using multiple threads.
6. Asynchronous programming does not necessarily require multiple threads.
7. Virtual threads make blocking-style programming more scalable.
8. WebFlux uses an asynchronous, non-blocking reactive model.

**Remember:**

```text
             CALLER                  THREAD
                │                       │
                ↓                       ↓
        Sync / Async             Blocking / Non-Blocking
                │                       │
                ↓                       ↓
       Does the caller            Does the current
        wait for result?           thread wait?
```

**One important distinction:**

```text
Virtual Threads + JDBC
    → Virtual thread waits
    → Carrier thread is released

WebFlux + R2DBC
    → Event-loop thread does not wait
    → Handles other work
    → Continues when DB response arrives
```
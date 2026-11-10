# Java Virtual Threads

## 1. Introduction to Threads

A **thread** is an independent path of execution within a process.

Java supports two types of threads:

1. Platform Threads
2. Virtual Threads

Virtual Threads were introduced as a final feature in **Java 21 (Project Loom)**.

---

## 2. Platform Threads

A **Platform Thread** is a Java thread backed by an Operating System (OS) thread.

The relationship is approximately 1:1.

```text
Java Platform Thread
        |
        | 1 : 1
        v
     OS Thread
        |
        v
    OS Scheduler
        |
        v
       CPU
```

**Important points:**

- Managed through the JVM and Operating System.
- Each Platform Thread is associated with one OS thread.
- Requires OS resources, including native stack memory.
- Creating a large number of Platform Threads can become expensive.

### Traditional Tomcat Architecture

By default, Tomcat uses Platform Threads for request processing.

```text
HTTP Request
     |
     v
   Tomcat
     |
     v
Platform Thread
     |
     v
Spring Security
     |
     v
DispatcherServlet
     |
     v
 Controller
     |
     v
  Service
     |
     v
 Repository
     |
     v
  Database
```

Usually, one request is processed by one Tomcat worker thread.

**Note:**
- Tomcat Thread describes the thread's role in Tomcat.
- Platform Thread describes the Java threading model.
- A traditional Tomcat worker thread is generally a Platform Thread.

---

## 3. Virtual Threads

A **Virtual Thread** is a lightweight Java thread managed primarily by the JVM.

Unlike Platform Threads, Virtual Threads are not permanently associated with individual OS threads.

Instead, the JVM schedules Virtual Threads onto Platform Threads called **Carrier Threads**.

```text
    Virtual Thread
          |
          v
    Carrier Thread
    (Platform Thread)
          |
          v
       OS Thread
          |
          v
          CPU
```

### How Multiple Virtual Threads Work

Multiple Virtual Threads can share a smaller number of Carrier Threads.

```text
VT-A ----\
VT-B -----\
VT-C ------> Carrier Thread 1 ---> OS Thread 1
VT-D -----/

VT-E ----\
VT-F -----> Carrier Thread 2 ---> OS Thread 2
VT-G ----/
```

The actual number of Carrier Threads depends on JVM scheduling and configuration.

### Why Are Virtual Threads Lightweight?

There are four main reasons:

1. **JVM-managed scheduling:** The JVM schedules Virtual Threads instead of requiring the OS to schedule each Virtual Thread individually.

2. **Lower memory overhead:** Virtual Threads use dynamically growing stack chunks stored in heap memory rather than requiring a large native stack for every thread.

3. **Carrier Thread reuse:** When a Virtual Thread parks during a supported blocking operation, its Carrier Thread can execute another Virtual Thread.

4. **Low creation cost:** Virtual Threads are relatively cheap to create and maintain compared with Platform Threads.

---

## 4. Carrier Thread, Mounting and Unmounting

A **Carrier Thread** is a Platform Thread that executes a Virtual Thread.

A Virtual Thread does not own a Carrier Thread permanently.

### Mounted

A Virtual Thread is mounted when it is currently executing on a Carrier Thread.

```text
Virtual Thread
      |
      v
Carrier Thread
      |
      v
   OS Thread
      |
      v
     CPU
```

### Unmounted

A Virtual Thread is unmounted when it is no longer occupying its Carrier Thread.

This commonly happens when it parks during a supported blocking operation.

```text
Virtual Thread
      |
      v
 Blocking I/O
      |
      v
     PARK
      |
      v
   UNMOUNT
      |
      v
Carrier becomes available
```

### Important: A Virtual Thread Can Change Carriers

For example:

Before blocking:

```text
VT-A
  |
  v
Carrier-1
  |
  v
OS Thread-1
```

After parking and later resuming:

```text
VT-A
  |
  v
Carrier-2
  |
  v
OS Thread-2
```

Therefore, a Virtual Thread is not permanently tied to a particular Carrier Thread.

### What Does Park Mean?

**Park means temporarily suspending a Virtual Thread because it cannot currently make progress.**

Important points:

- The Virtual Thread is not destroyed.
- Its execution state is retained.
- Its Carrier Thread can execute other Virtual Threads.
- When it becomes runnable again, the JVM schedules it onto an available Carrier Thread.

---

## 5. Why Do We Need Virtual Threads?

The main purpose of Virtual Threads is to improve concurrency, especially for applications that spend significant time waiting for I/O operations.

Consider a typical backend application.

```text
HTTP Request
     |
     v
   Service
     |
     v
  Database
     |
     v
    WAIT
```

The CPU may not have much work to do while waiting for the database response.

Common I/O operations include:

- Database queries
- REST API calls
- File I/O
- Network I/O
- Message broker communication

### Platform Thread Problem

Suppose we have:

```text
2 Platform Threads
10 Incoming Requests
```

The first two requests occupy both available threads.

```text
Platform Thread 1
       |
       v
    Request A
       |
       v
     DB Call
       |
       v
      WAIT


Platform Thread 2
       |
       v
    Request B
       |
       v
     DB Call
       |
       v
      WAIT
```

Requests C to J cannot start processing until a worker thread becomes available.

The problem is **Platform Thread Pool Exhaustion**.

The threads are occupied even though they are waiting for database responses.

### Virtual Threads Solution

Now consider:

```text
1000 Requests
     |
     v
1000 Virtual Threads
     |
     v
Small Number of Carrier Threads
```

For example:

```text
VT-A -> DB -> PARK
VT-B -> DB -> PARK
VT-C -> CPU -> RUN
VT-D -> DB -> PARK
VT-E -> HTTP -> PARK
VT-F -> CPU -> RUN
```

When VT-A parks, its Carrier Thread can execute another runnable Virtual Thread.

This improves Carrier Thread utilization and allows more concurrent requests.

---

## 6. Platform Thread vs Virtual Thread During I/O

### Platform Thread

```text
Platform Thread
      |
      v
  Database Call
      |
      v
  WAIT for 5 seconds
      |
      v
OS Thread remains occupied
      |
      v
Database Response
      |
      v
  Continue execution
```

### Virtual Thread

```text
Virtual Thread
      |
      v
  Database Call
      |
      v
     PARK
      |
      v
   UNMOUNT
      |
      v
Carrier becomes available
      |
      v
Another Virtual Thread executes
      |
      v
Database Response
      |
      v
VT becomes runnable
      |
      v
VT mounts on a Carrier Thread
      |
      v
  Continue execution
```

**Key difference:**

A blocked Platform Thread continues occupying its OS thread, whereas a Virtual Thread can park and release its Carrier Thread during supported blocking operations.

---

## 7. Java Virtual Thread Example

Java 21 introduced Virtual Threads as a production feature.

### Creating a Platform Thread

```java
public class PlatformThreadExample {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> System.out.println(Thread.currentThread()));
        thread.start();
    }
}
```

### Creating a Virtual Thread

```java
public class VirtualThreadExample {
    public static void main(String[] args) {
        Thread thread = Thread.startVirtualThread(() -> System.out.println(Thread.currentThread()));
    }
}
```

### Using Virtual Thread Executor

```java
import java.util.concurrent.Executors;

public class VirtualThreadExecutorExample {
    public static void main(String[] args) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1000; i++) {
                executor.submit(() -> {
                    // Simulating a blocking operation
                    Thread.sleep(1000);
                    System.out.println(Thread.currentThread());
                    return null;
                });
            }
        }
    }
}
```

**Important:**

- Each submitted task gets its own Virtual Thread.
- Thousands of Virtual Threads can be created without requiring thousands of OS threads.
- Virtual Threads should generally not be pooled just to limit their number.

---

## 8. Spring Boot + Virtual Threads

Spring Boot 3.2+ supports Virtual Threads when running on Java 21 or later.

Enable them using:

```properties
spring.threads.virtual.enabled=true
```

For supported embedded Tomcat configurations, request processing can use Virtual Threads.

### Traditional Tomcat

```text
HTTP Request
     |
     v
   Tomcat
     |
     v
Platform Thread
     |
     v
Spring Security
     |
     v
DispatcherServlet
     |
     v
 Controller
     |
     v
  Service
     |
     v
 Repository
     |
     v
  Database
```

### Tomcat with Virtual Threads

```text
HTTP Request
     |
     v
   Tomcat
     |
     v
Virtual Thread
     |
     v
Spring Security
     |
     v
DispatcherServlet
     |
     v
 Controller
     |
     v
  Service
     |
     v
 Repository
     |
     v
  Database
```

The Virtual Thread executes on a Carrier Thread, which is a Platform Thread backed by an OS thread.

### Practical Example

Controller:

```java
@GetMapping("/customer/{id}")
public Customer getCustomer(@PathVariable Long id) {
    return customerService.getCustomer(id);
}
```

Service:

```java
public Customer getCustomer(Long id) {
    return customerRepository.findById(id).orElseThrow();
}
```

Suppose the database takes 5 seconds to respond.

```text
HTTP Request
     |
     v
Virtual Thread A
     |
     v
 Controller
     |
     v
  Service
     |
     v
 Repository
     |
     v
  Database
     |
     v
    PARK
```

During the wait, the Carrier Thread can execute another runnable Virtual Thread.

When the database response arrives:

```text
Database Response
       |
       v
VT-A becomes runnable
       |
       v
JVM schedules VT-A
       |
       v
VT-A mounts on a Carrier Thread
       |
       v
Execution continues
       |
       v
Controller returns response
```

The application code can remain synchronous. You do not necessarily need to rewrite it using CompletableFuture, callbacks, Mono or Flux.

---

## 9. Important Considerations

### A. CPU-Bound vs I/O-Bound

Virtual Threads are especially useful for I/O-bound workloads.

**CPU-Bound Work**

Example:

```java
public void calculate() {

    long sum = 0;

    for (long i = 0; i < 1_000_000_000L; i++) {
        sum += i;
    }
}
```

The thread continuously performs calculations.

```text
CPU-Bound Work
      |
      v
Needs CPU
      |
      v
CPU Cores Are the Limiting Resource
```

Virtual Threads do not magically increase CPU processing capacity.

**I/O-Bound Work**

Example:

```text
Request
   |
   v
Database
   |
   v
  WAIT
   |
   v
Response
```

```text
I/O-Bound Work
      |
      v
Lots of Waiting
      |
      v
Virtual Threads Can Park
      |
      v
Carrier Threads Can Execute Other Work
```

This is where Virtual Threads provide significant concurrency benefits.

### B. Thread Pinning

**Thread pinning** occurs when a Virtual Thread cannot unmount from its Carrier Thread while blocked.

For example, certain native or foreign function calls can pin a Virtual Thread.

```text
Virtual Thread
      |
      v
Blocking Operation
      |
      v
    PINNED
      |
      v
Cannot Unmount
      |
      v
Carrier Remains Occupied
```

**Important Java version distinction:**

- Java 21 to Java 23: Blocking inside certain `synchronized` sections could pin Virtual Threads.
- Java 24+: JEP 491 improved this behavior by allowing Virtual Threads to unmount in synchronized code in situations that previously caused pinning.
- Native and foreign function calls can still cause pinning.

Therefore, it is incorrect to say that every `synchronized` block pins a Virtual Thread.

### C. Database Connection Pool Limitations

Suppose:

```text
100,000 Virtual Threads
          |
          v
       Database
          |
          v
   Connection Pool
          |
          v
   Maximum 20 Connections
```

Only around 20 operations can use those database connections simultaneously.

Virtual Threads do not create additional database connections.

### D. External Service Limitations

Suppose an external API supports only 100 concurrent requests.

```text
100,000 Virtual Threads
          |
          v
     External API
          |
          v
 Maximum Concurrency = 100
```

The external system remains the bottleneck.

### E. Lock Contention

If thousands of Virtual Threads compete for the same lock:

```text
VT-A -> Acquires LOCK
VT-B -> WAIT
VT-C -> WAIT
VT-D -> WAIT
```

Virtual Threads do not eliminate lock contention.

### F. ThreadLocal

Virtual Threads support ThreadLocal.

However, avoid unnecessarily storing large objects in ThreadLocal because applications may create a very large number of Virtual Threads.

Also, remember that Virtual Threads can move between Carrier Threads.

```text
Virtual Thread A
       |
       v
   Carrier-1
       |
     PARK
       |
       v
   Carrier-2
```

Do not use Carrier Thread identity to identify a request.

---

## 10. Final Comparison and Mental Model

### Platform Thread vs Virtual Thread

| Feature                 | Platform Thread                           | Virtual Thread                                   |
|-------------------------|-------------------------------------------|--------------------------------------------------|
| Managed primarily by    | OS + JVM                                  | JVM                                              |
| Backed by OS thread     | Yes                                       | Through Carrier Thread                           |
| OS mapping              | Approximately 1:1                         | Many-to-few                                      |
| Stack memory            | Native stack                              | Heap-based stack chunks                          |
| Creation cost           | Relatively high                           | Relatively low                                   |
| Memory overhead         | Higher                                    | Lower                                            |
| Large-scale concurrency | More expensive                            | More practical                                   |
| Blocking I/O            | Occupies OS thread                        | Can park and unmount                             |
| CPU-bound performance   | Depends on CPU                            | Does not magically improve                       |
| Carrier Thread          | Not required                              | Required                                         |
| Can change carrier      | Not applicable                            | Yes                                              |
| Best suited for         | General-purpose and CPU-limited workloads | High-concurrency, especially I/O-heavy workloads |

### Important Thread Relationships

```text
                   JVM
                    |
          +---------+---------+
          |                   |
          v                   v
   Platform Threads      Virtual Threads
          |                   |
          |                   v
          |             Carrier Threads
          |                   |
          v                   v
       OS Threads          OS Threads
          |                   |
          +---------+---------+
                    |
                    v
               OS Scheduler
                    |
                    v
                   CPU
```

### Six Important Definitions

**1. Platform Thread**

A Java thread backed by an OS/native thread.

**2. OS Thread**

A thread managed and scheduled by the Operating System.

**3. Tomcat Thread**

A Tomcat worker thread used to process an HTTP request. Traditionally, it is a Platform Thread.

**4. Virtual Thread**

A lightweight Java thread managed by the JVM that is not permanently tied to an OS thread.

**5. Carrier Thread**

A Platform Thread that executes a Virtual Thread.

**6. Native Thread**

An OS-level thread managed through the operating system's native threading mechanism.

---

### Final Summary

**Platform Thread**

```text
1 Java Platform Thread
        |
        v
   1 OS Thread
        |
        v
   Blocking I/O
        |
        v
OS Thread remains occupied
```

**Virtual Thread**

```text
Many Virtual Threads
        |
        v
Fewer Carrier Threads
        |
        v
     OS Threads
        |
        v
  Blocking I/O
        |
        v
Virtual Thread can PARK
        |
        v
     UNMOUNT
        |
        v
Carrier becomes available
        |
        v
Another Virtual Thread executes
```

---

# Questions and Answers

### Q1. Are virtual threads dynamic in nature? Do they increase or decrease based on traffic?

**Answer:**

Yes. Virtual threads are lightweight and created dynamically for individual tasks.

- When concurrent requests increase, more virtual threads are created.
- When requests complete, their virtual threads terminate.
- When traffic decreases, the number of active virtual threads also decreases.

**Example:**

```text
10 concurrent requests
        |
        v
10 virtual threads

1000 concurrent requests
        |
        v
1000 virtual threads

Requests completed
        |
        v
Virtual threads terminate
```

### Q2. Are virtual threads created from a thread pool?

**Answer:**

No. Virtual threads are not pooled and reused like traditional platform threads.

A new virtual thread is generally created for each task, and it terminates when the task completes.

### Q3. If 1000 virtual threads are created, does the JVM create 1000 OS threads?

**Answer:**

No. Virtual threads are not directly mapped one-to-one to OS threads.

The JVM schedules many virtual threads onto a smaller number of carrier platform threads.

```text
       1000 Virtual Threads
                |
                v
        JVM Scheduler
                |
       +--------+--------+
       |        |        |
       v        v        v
    Carrier  Carrier  Carrier
    Thread 1 Thread 2 Thread 3
       |        |        |
       +--------+--------+
                |
                v
           OS Threads
                |
                v
              CPU
```

### Q4. What is a carrier thread?

**Answer:**

A carrier thread is a platform thread that executes a virtual thread.

The JVM scheduler assigns virtual threads to available carrier threads.

A carrier thread can execute different virtual threads over time.

### Q5. What happens when a virtual thread performs blocking I/O?

**Answer:**

For supported blocking operations:

1. The virtual thread gets parked while waiting for I/O.
2. It is unmounted from its carrier thread.
3. The carrier thread becomes available to execute another virtual thread.
4. When the I/O operation completes, the virtual thread can resume on an available carrier thread.

This improves scalability when applications perform many blocking I/O operations.

### Q6. Are virtual threads unlimited?

**Answer:**

No. Although virtual threads are lightweight, they still consume memory and system resources.

Creating too many virtual threads can cause resource pressure.

For example, database connection pools and external services can still become bottlenecks.

### Q7. Does increasing traffic increase the number of carrier threads automatically?

**Answer:**

Not necessarily.

Virtual threads and carrier threads have different responsibilities:

- Virtual threads are created per task.
- Carrier threads are managed by the JVM scheduler.
- Many virtual threads can share a smaller number of carrier threads.

The number of virtual threads can grow significantly without creating an equal number of carrier threads.

### Q8. What is the difference between platform threads and virtual threads regarding thread management?

**Answer:**

| Platform Threads                    | Virtual Threads                         |
|-------------------------------------|-----------------------------------------|
| Relatively heavyweight              | Lightweight                             |
| Commonly managed using thread pools | Created per task                        |
| Pool threads are reused             | Virtual threads are not pooled          |
| Usually backed by OS threads        | Scheduled onto carrier platform threads |
| Limited by OS thread resources      | Can support much higher concurrency     |

### Q9. Do virtual threads improve application performance?

**Answer:**

Virtual threads primarily improve scalability for I/O-bound workloads.

They allow an application to handle many concurrent tasks without requiring one platform thread per task.

However, they do not make CPU-intensive operations execute faster.

### Q10. If a virtual thread completes its task, is it reused for another request?

**Answer:**

No. A completed virtual thread terminates.

A new virtual thread is created for another task. The carrier platform thread, however, can be reused to execute other virtual threads.

---

### Important Interview Takeaway

- Virtual threads are created per task.
- Their active count generally increases or decreases with concurrent workload.
- They are not pooled or reused.
- Multiple virtual threads share carrier platform threads.
- Carrier threads are backed by OS threads.
- Virtual threads improve concurrency, especially for I/O-bound workloads.
- They do not eliminate resource limits such as database connections or CPU capacity.

---

### Key Takeaway

> Platform Threads map approximately 1:1 to OS threads, whereas many Virtual Threads can share a smaller number of Carrier Threads. This allows applications to handle very high concurrency efficiently, especially when tasks spend significant time waiting for I/O.

**Remember:**

Virtual Threads improve concurrency, not CPU capacity. They do not remove bottlenecks such as CPU limitations, database connection pools, external service limits or lock contention.
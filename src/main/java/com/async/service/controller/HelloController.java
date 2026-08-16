package com.async.service.controller;

import com.async.service.entity.ProductsResult;
import com.async.service.service.AsyncApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HelloController {

    @Autowired
    AsyncApiService asyncApiService;

    // This API is blocking in nature.
    // Thread.sleep(1000) blocks/occupies the Tomcat worker thread for ~1 second.
    // With multiple requests, available Tomcat threads determine how many can execute concurrently.
    // Example: 10 requests + 1 thread -> sequential (~10s); 10 requests + 10 threads -> concurrent (~1s).
    // http://localhost:3001/hello
    @GetMapping("/hello")
    public String hello() throws InterruptedException {

        // Tomcat worker thread handling this request.
        System.out.println("Hello Thread : " + Thread.currentThread().getName());

        // Blocking operation: worker thread is occupied for ~1 second.
        Thread.sleep(1000);

        // If 10 requests arrive simultaneously:
        // 1 Tomcat thread  -> requests execute one by one -> ~10 seconds.
        // 10 Tomcat threads -> 10 requests execute concurrently -> ~1 second.

        long sum = 0L;
        for (int i = 1; i <= 10; i++) {
            sum = sum + i;
        }

        return "Hello World!";
    }

    // API invocation in an API
    // 2 Ways -> Synchronously - RestTemplate
    //           Asynchronously - testTemplate and Webclient

    // http://localhost:3001/hello2
    @GetMapping("hello2")
    public String hello2() {

        // Tomcat worker thread handling this request.
        System.out.println("Hello2 Thread : " + Thread.currentThread().getName());

        // Calls another API from this request.
        // This is a blocking call if the downstream API also requires a Tomcat worker thread.
        //
        // Observation:
        // If Tomcat has only 1 worker thread:
        //   Request -> hello2 -> waits for another API -> needs a worker thread
        //   But the only worker thread is already occupied by hello2 -> request can block/deadlock.
        //
        // Therefore, for synchronous API-to-API calls, you need at least 2 available threads
        // when the downstream API is handled by the same server/thread pool.
        //
        // With 2+ threads:
        //   Thread-1 -> hello2 -> waits for downstream API
        //   Thread-2 -> handles downstream API -> returns response
        //
        // Key concept: A blocking downstream call occupies the current Tomcat worker thread.
        return asyncApiService.invokeHelloEndPoint();
    }

    // http://localhost:3001/products-sync
    @GetMapping("products-sync")
    public ProductsResult fetchProductsSync() {

        // Tomcat worker thread handling this request.
        System.out.println("FetchProductsSync : " + Thread.currentThread().getName());

        // Synchronous call: the current Tomcat thread waits until the operation completes.
        // If the downstream operation is blocking, the Tomcat worker thread remains occupied.
        //
        // Multiple requests require multiple Tomcat threads for concurrent execution.
        // Example: 10 requests + 1 thread -> one-by-one; 10 requests + 10 threads -> concurrently.
        return asyncApiService.fetchProductsSync();
    }

    // http://localhost:3001/products-async
    // Mono / Flux / Future / CompletableFuture represent a result that will be available later.
    @GetMapping("products-async")
    public Mono<ProductsResult> fetchProductsASync() {

        // Tomcat worker thread handling the initial request.
        System.out.println("FetchProductsASync : " + Thread.currentThread().getName());

        // Returns immediately with a Mono instead of waiting for the result.
        // The actual result is produced asynchronously when it becomes available.
        //
        // Key concept:
        // Synchronous -> thread waits for the result.
        // Asynchronous -> thread can be released while waiting for the result.
        return asyncApiService.fetchProductsASync();
    }

}

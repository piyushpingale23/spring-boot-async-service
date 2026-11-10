package com.async.service.service;

import com.async.service.entity.ProductsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.List;

@Service
public class AsyncApiService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    WebClient webClient;

    public static final String DUMMY_JSON_API = "https://dummyjson.com/products";

    public String invokeHelloEndPoint() {

        return restTemplate.getForObject("http://localhost:3001/hello", String.class);

    }

    public ProductsResult fetchProductsSync() {

        return restTemplate.getForObject(DUMMY_JSON_API, ProductsResult.class);

    }

    public Mono<ProductsResult> fetchProductsASync() {

        Mono<ProductsResult> productsResults = webClient.get().uri(DUMMY_JSON_API).retrieve()
                .bodyToMono(ProductsResult.class)
                .doOnSuccess(products -> {
                    System.out.println("Reactor Thread : " + Thread.currentThread().getName());
                    System.out.println("Response Received : " + products);
                })
                .doOnError(error -> System.out.println("Error : " + error));

        long sum = 0L;
        for (int i = 1; i <= 10; i++) {
            sum = sum + i;
        }

        return productsResults;

    }

    public Mono<List<ProductsResult>> fetchProductsASyncParallelAll() {

        // Create the reactive pipeline for API request 1.
        // At this point, the HTTP request is NOT executed yet.
        // The Mono represents the future result of this API call.
        Mono<ProductsResult> result1 = getMonoProductResult(1);

        // Create the reactive pipeline for API request 2.
        // The HTTP request is NOT executed yet.
        Mono<ProductsResult> result2 = getMonoProductResult(2);

        // Create the reactive pipeline for API request 3.
        // The HTTP request is NOT executed yet.
        Mono<ProductsResult> result3 = getMonoProductResult(3);

        // Tomcat Thread
        System.out.println("fetchProductsASyncParallelAll Service : " + Thread.currentThread().getName());

        // Mono.zip subscribes to all three Mono and waits for ALL of them to successfully produce a result.
        //
        // API-1 ───────┐
        // API-2 ───────┼──> Mono.zip ──> List<ProductsResult>
        // API-3 ───────┘
        //
        // The downstream API calls are initiated concurrently.
        //
        // If ALL APIs succeed:
        //     -> tuple containing all 3 results
        //     -> converted into List<ProductsResult>
        //
        // If ANY API fails:
        //     -> Mono.zip fails
        //     -> successful combined result is NOT returned.
        return Mono.zip(result1, result2, result3)

                // Combine the three API responses into a single List.
                .map(tuple -> List.of(tuple.getT1(), tuple.getT2(), tuple.getT3()))

                // Executes when the complete pipeline successfully finishes.
                // This may execute on a Reactor Netty/Event Loop thread.
                .doOnSuccess(result -> System.out.println("fetchProductsASyncParallelAll request completed by thread: " + Thread.currentThread().getName()))

                // Executes if any of the parallel API calls causes the
                // combined Mono to fail.
                .doOnError(error -> System.out.println("Parallel Api invocation failed! " + error)
                );
    }


    public Mono<ProductsResult> getMonoProductResult(int call) {

        return webClient.get()
                .uri(DUMMY_JSON_API)
                .retrieve()
                .bodyToMono(ProductsResult.class)

                // Executes when the HTTP response has successfully arrived
                // and has been converted into ProductsResult.
                //
                // This is generally executed by the Reactor Netty event-loop
                // thread responsible for the network operation.
                .doOnSuccess(products -> {
                    System.out.println("Request " + call + " executed by thread: " + Thread.currentThread().getName());
                    System.out.println("Response of request " + call + ": " + products);
                })

                // Executes when this API call fails.
                .doOnError(error -> System.out.println("Error : " + error))

                // Subscription is done by Tomcat thread, and it frees to serve another request
                .doOnSubscribe(subscription ->
                        System.out.println("Request " + call + " subscribed by thread " + Thread.currentThread().getName())
                );
    }

    public Mono<ProductsResult> fetchProductsASyncParallelFastest() {

        Mono<ProductsResult> result1 = getMonoProductResult(1);

        Mono<ProductsResult> result2 = getMonoProductResult(2);

        Mono<ProductsResult> result3 = getMonoProductResult(3);

        // Tomcat Thread
        System.out.println("fetchProductsASyncParallelFastest Service : " + Thread.currentThread().getName());

        return Mono.firstWithSignal(result1, result2, result3)
                .doOnSuccess(result -> System.out.println("fetchProductsASyncParallelFastest request completed by thread: " + Thread.currentThread().getName()))
                .doOnError(error -> System.out.println("Parallel fastest Api invocation failed! " + error)
                );
    }

    public List<ProductsResult> fetchProductsChainedSync() {

        System.out.println("fetchProductsChainedSync Controller : " + Thread.currentThread().getName());

        ProductsResult result1 = restTemplate.getForObject(DUMMY_JSON_API, ProductsResult.class);
        System.out.println("Request 1 executed by thread: " + Thread.currentThread().getName());

        ProductsResult result2 = webClient.get().uri(DUMMY_JSON_API).retrieve().bodyToMono(ProductsResult.class).block();
        System.out.println("Request 2 executed by thread: " + Thread.currentThread().getName());

        ProductsResult result3 = restTemplate.getForObject(DUMMY_JSON_API, ProductsResult.class);
        System.out.println("Request 3 executed by thread: " + Thread.currentThread().getName());

        assert result1 != null;
        assert result2 != null;
        assert result3 != null;
        return List.of(result1, result2, result3);

    }

    public Mono<List<ProductsResult>> fetchProductsChainedASync() {

        System.out.println("fetchProductsChainedASync Service : " + Thread.currentThread().getName());

        return getMonoProductResult(1).flatMap(result1 -> {
            return getMonoProductResult(2).flatMap(result2 -> {
                return getMonoProductResult(3)
                        .map(result3 -> List.of(result1, result2, result3));
            });
        });
    }

    public Flux<ProductsResult> getProductsFluxStream() {

        return Flux.interval(Duration.ofSeconds(5)).take(20).flatMap(i -> getMonoProductResult(Math.toIntExact(i)));
    }
}

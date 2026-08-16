package com.async.service.service;

import com.async.service.entity.ProductsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AsyncApiService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    WebClient webClient;

    public String invokeHelloEndPoint() {

        return restTemplate.getForObject("http://localhost:3001/hello", String.class);

    }

    public ProductsResult fetchProductsSync() {

        return restTemplate.getForObject("https://dummyjson.com/products", ProductsResult.class);

    }

    public Mono<ProductsResult> fetchProductsASync() {

        Mono<ProductsResult> productsResults = webClient.get().uri("https://dummyjson.com/products").retrieve()
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
}

package com.async.service.entity;

import java.util.List;

public class ProductsResult {

    private List<Products> products;

    public ProductsResult() {
    }

    public ProductsResult(List<Products> products) {
        this.products = products;
    }

    public List<Products> getProducts() {
        return products;
    }

    public void setProducts(List<Products> products) {
        this.products = products;
    }

}

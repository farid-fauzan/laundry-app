package com.laundry.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Order not found with id: " + id);
    }
    public OrderNotFoundException(String orderCode) {
        super("Order not found with code: " + orderCode);
    }
}

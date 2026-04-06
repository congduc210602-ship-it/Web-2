package com.rainbowforest.orderservice.service;

import java.util.List;

import com.rainbowforest.orderservice.domain.Order;

public interface OrderService {
    public Order saveOrder(Order order);
    public List<Order> getAllOrders();
    public Order getOrderById(Long id);
}

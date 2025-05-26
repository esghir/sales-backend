package com.sales.backend.service;

import com.sales.backend.model.*;
import com.sales.backend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    public Order createOrderFromCart(Long userId) {
        Cart cart = cartService.getCartByUserId(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot create order from empty cart");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(cart.getTotalAmount()));

        // Convert cart items to order items
        cart.getItems().forEach(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setSubtotal(cartItem.getSubtotal());
            order.getItems().add(orderItem);

            // Update product stock
            Product product = cartItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
        });

        // Save order and clear cart
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);
        return savedOrder;
    }

    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public void cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }

        // Restore product stock
        order.getItems().forEach(item -> {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        });

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
} 
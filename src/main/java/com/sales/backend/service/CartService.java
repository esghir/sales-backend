package com.sales.backend.service;

import com.sales.backend.model.Cart;
import com.sales.backend.model.CartItem;
import com.sales.backend.model.Product;
import com.sales.backend.repository.CartRepository;
import com.sales.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    public Cart getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            throw new RuntimeException("Cart not found for user: " + userId);
        }
        return cart;
    }

    public Cart addItemToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            throw new RuntimeException("Cart not found for user: " + userId);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for product: " + productId);
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setPrice(product.getPrice());
            newItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            cart.getItems().add(newItem);
        }

        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    public Cart removeItemFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            throw new RuntimeException("Cart not found for user: " + userId);
        }

        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    public Cart updateItemQuantity(Long userId, Long productId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            throw new RuntimeException("Cart not found for user: " + userId);
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart: " + productId));

        if (item.getProduct().getStockQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for product: " + productId);
        }

        item.setQuantity(quantity);
        item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            throw new RuntimeException("Cart not found for user: " + userId);
        }

        cart.getItems().clear();
        cart.setTotalAmount(0.0);
        cartRepository.save(cart);
    }

    private void updateCartTotal(Cart cart) {
        double total = cart.getItems().stream()
                .mapToDouble(item -> item.getSubtotal().doubleValue())
                .sum();
        cart.setTotalAmount(total);
    }
} 
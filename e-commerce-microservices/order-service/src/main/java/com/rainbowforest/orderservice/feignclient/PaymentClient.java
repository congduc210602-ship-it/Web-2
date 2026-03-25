package com.rainbowforest.orderservice.feignclient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

@FeignClient(name = "payment-service") // Trỏ sang service mới
public interface PaymentClient {
    @PostMapping("/payment/process")
    ResponseEntity<String> processPayment(
            @RequestParam("userId") Long userId,
            @RequestParam("orderId") Long orderId,
            @RequestParam("amount") BigDecimal amount);
}
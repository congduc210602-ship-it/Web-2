package com.rainbowforest.paymentservice.feignclient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

@FeignClient(name = "user-service")
public interface UserClient {
    @PostMapping("/users/{id}/deduct-balance")
    ResponseEntity<String> deductBalance(@PathVariable("id") Long id, @RequestParam("amount") BigDecimal amount);
}
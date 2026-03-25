package com.rainbowforest.paymentservice.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long orderId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String status;

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; } 
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOrderId() { return orderId; } 
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; } 
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getPaymentDate() { return paymentDate; } 
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public String getStatus() { return status; } 
    public void setStatus(String status) { this.status = status; }
}
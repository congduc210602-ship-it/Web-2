package com.rainbowforest.paymentservice.repository;

import com.rainbowforest.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
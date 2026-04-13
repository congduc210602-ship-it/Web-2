package com.rainbowforest.promotionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rainbowforest.promotionservice.entity.Promotion;

import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    // Tìm mã giảm giá dựa vào Code và phải đang ở trạng thái Active
    Optional<Promotion> findByCodeAndActiveTrue(String code);
}
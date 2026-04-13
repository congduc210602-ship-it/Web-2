package com.rainbowforest.promotionservice.service;

import com.rainbowforest.promotionservice.entity.Promotion;
import com.rainbowforest.promotionservice.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Override
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    @Override
    public Promotion createPromotion(Promotion promotion) {
        return promotionRepository.save(promotion);
    }

    // Hàm kiểm tra tính hợp lệ của mã giảm giá khi khách hàng nhập vào
    @Override
    public Promotion validateAndGetPromotion(String code) {
        Promotion promotion = promotionRepository.findByCodeAndActiveTrue(code).orElse(null);
        
        if (promotion != null) {
            LocalDateTime now = LocalDateTime.now();
            // Kiểm tra xem thời gian hiện tại có nằm trong thời gian khuyến mãi không
            if (now.isAfter(promotion.getStartDate()) && now.isBefore(promotion.getEndDate())) {
                return promotion; // Hợp lệ
            }
        }
        return null; // Không hợp lệ hoặc hết hạn
    }
}
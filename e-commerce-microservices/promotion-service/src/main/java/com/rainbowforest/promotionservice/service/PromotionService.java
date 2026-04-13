package com.rainbowforest.promotionservice.service;

import java.util.List;

import com.rainbowforest.promotionservice.entity.Promotion;

public interface PromotionService {
    List<Promotion> getAllPromotions();
    Promotion createPromotion(Promotion promotion);
    Promotion validateAndGetPromotion(String code);
}
package com.rainbowforest.recommendationservice.service;

import com.rainbowforest.recommendationservice.model.Recommendation;
import java.util.List;

public interface RecommendationService {
    Recommendation getRecommendationById(Long recommendationId);

    Recommendation saveRecommendation(Recommendation recommendation);

    List<Recommendation> getAllRecommendationByProductName(String productName);

    void deleteRecommendation(Long id);

    // === HÀM MỚI THÊM DÀNH CHO ADMIN ===
    List<Recommendation> getAllRecommendations();
}
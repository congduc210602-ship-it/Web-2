package com.rainbowforest.promotionservice.controller;

import com.rainbowforest.promotionservice.entity.Promotion;
import com.rainbowforest.promotionservice.http.header.HeaderGenerator;
import com.rainbowforest.promotionservice.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private HeaderGenerator headerGenerator;

    // ==========================================
    // LẤY DANH SÁCH KHUYẾN MÃI (CHO TRANG ADMIN)
    // ==========================================
    @GetMapping("/admin/promotions")
    public ResponseEntity<List<Promotion>> getAllPromotions() {
        List<Promotion> list = promotionService.getAllPromotions();
        
        // Luôn trả về 200 OK kèm danh sách (kể cả rỗng) để Frontend không bị lỗi 404
        return new ResponseEntity<List<Promotion>>(
                list,
                headerGenerator.getHeadersForSuccessGetMethod(),
                HttpStatus.OK);
    }

    // ==========================================
    // TẠO MỚI KHUYẾN MÃI (CHO TRANG ADMIN)
    // ==========================================
    @PostMapping("/admin/promotions")
    public ResponseEntity<Promotion> createPromotion(@RequestBody Promotion promotion, HttpServletRequest request) {
        try {
            Promotion saved = promotionService.createPromotion(promotion);
            return new ResponseEntity<Promotion>(
                    saved,
                    headerGenerator.getHeadersForSuccessPostMethod(request, saved.getId()),
                    HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<Promotion>(
                    headerGenerator.getHeadersForError(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==========================================
    // KIỂM TRA MÃ GIẢM GIÁ (CHO NGƯỜI DÙNG Ở GIỎ HÀNG)
    // ==========================================
    @GetMapping("/validate/{code}")
    public ResponseEntity<Promotion> validatePromotion(@PathVariable("code") String code) {
        Promotion validPromotion = promotionService.validateAndGetPromotion(code);
        if (validPromotion != null) {
            return new ResponseEntity<Promotion>(
                    validPromotion,
                    headerGenerator.getHeadersForSuccessGetMethod(),
                    HttpStatus.OK);
        }
        return new ResponseEntity<Promotion>(
                headerGenerator.getHeadersForError(),
                HttpStatus.NOT_FOUND); // 404 nếu nhập sai mã hoặc hết hạn
    }
}
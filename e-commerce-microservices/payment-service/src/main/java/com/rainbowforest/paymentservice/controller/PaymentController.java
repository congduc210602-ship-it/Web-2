package com.rainbowforest.paymentservice.controller;

import com.rainbowforest.paymentservice.config.VNPayConfig;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;

import com.rainbowforest.paymentservice.entity.Payment;
import com.rainbowforest.paymentservice.feignclient.UserClient;
import com.rainbowforest.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private UserClient userClient;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/process")
    public ResponseEntity<String> processPayment(
            @RequestParam("userId") Long userId,
            @RequestParam("orderId") Long orderId,
            @RequestParam("amount") BigDecimal amount) {

        try {
            ResponseEntity<String> response = userClient.deductBalance(userId, amount);

            if (response.getStatusCode() == HttpStatus.OK) {
                Payment payment = new Payment();
                payment.setUserId(userId);
                payment.setOrderId(orderId);
                payment.setAmount(amount);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setStatus("SUCCESS");
                paymentRepository.save(payment);

                return new ResponseEntity<>("PAYMENT_SUCCESS", HttpStatus.OK);
            }
        } catch (Exception e) {
            System.out.println("Thanh toan that bai hoac User Service báo loi: " + e.getMessage());
        }
        return new ResponseEntity<>("PAYMENT_FAILED", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/create-vnpay")
    public ResponseEntity<String> createVNPayPayment(
            @RequestParam("amount") long amount,
            @RequestParam("orderId") String orderId,
            HttpServletRequest req) throws UnsupportedEncodingException {

        String vnp_TxnRef = orderId + "_" + System.currentTimeMillis();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_Returnurl);
        // Đã sửa lại để lấy IP thực tế thay vì hardcode 127.0.0.1
        vnp_Params.put("vnp_IpAddr", VNPayConfig.getIpAddress(req)); 

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        // Sắp xếp tham số theo Alphabet
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        
        // Vòng lặp chuẩn VNPay dùng Iterator để tránh lỗi dư dấu &
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return ResponseEntity.ok(VNPayConfig.vnp_PayUrl + "?" + queryUrl);
    }

    // Endpoint nhận kết quả trả về từ VNPay
    @GetMapping("/vnpay-callback")
    public ResponseEntity<String> vnpayCallback(HttpServletRequest request) {
        String status = request.getParameter("vnp_ResponseCode");
        if ("00".equals(status)) {
            // THANH TOÁN THÀNH CÔNG
            // Ở đây Đức có thể gọi Repository lưu vào DB Payment với status SUCCESS
            return ResponseEntity.ok("THANH TOAN THANH CONG!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("THANH TOAN THAT BAI!");
        }
    }
}
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
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
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
        vnp_Params.put("vnp_OrderInfo", "ThanhToan_" + orderId);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_Returnurl);

        // Gắn cứng IP localhost để tránh lỗi IPv6 (0:0:0:0:0:0:0:1) trên máy dev
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);

            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // BẮT BUỘC ENCODE CẢ CHUỖI HASH VÀ QUERY
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());

                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(encodedValue);

                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(encodedValue);

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());

        // TUYỆT ĐỐI KHÔNG NỐI THÊM vnp_SecureHashType VÀO ĐÂY NỮA
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;

        return ResponseEntity.ok(paymentUrl);
    }

    // Endpoint nhận kết quả trả về từ VNPay
    @GetMapping("/vnpay-callback")
    public void vnpayCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Map<String, String> fields = new HashMap<>();

        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();

        for (String name : fieldNames) {
            hashData.append(name).append('=').append(fields.get(name)).append('&');
        }

        hashData.deleteCharAt(hashData.length() - 1);

        String signValue = VNPayConfig.hmacSHA512(
                VNPayConfig.vnp_HashSecret,
                hashData.toString());

        // === ĐƯỜNG DẪN FRONTEND CỦA BẠN ===
        String frontendUrl = "http://localhost:3000/cart";

        // === SỬA LẠI PHẦN TRẢ VỀ: DÙNG SEND REDIRECT ===
        if (signValue.equals(vnp_SecureHash)) {
            String responseCode = request.getParameter("vnp_ResponseCode");
            if ("00".equals(responseCode)) {
                // THANH TOÁN THÀNH CÔNG
                // TODO: Gọi Repository lưu DB trạng thái "SUCCESS" ở đây

                // Đá về React kèm param success
                response.sendRedirect(frontendUrl + "?payment_status=success");
            } else {
                // Khách hàng hủy giao dịch hoặc lỗi thẻ
                response.sendRedirect(frontendUrl + "?payment_status=failed");
            }
        } else {
            // Chữ ký không hợp lệ (Bị hacker sửa URL)
            response.sendRedirect(frontendUrl + "?payment_status=invalid");
        }
    }
}
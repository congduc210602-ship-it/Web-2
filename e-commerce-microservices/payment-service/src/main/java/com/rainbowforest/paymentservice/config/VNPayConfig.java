package com.rainbowforest.paymentservice.config;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayConfig {
    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_Returnurl = "http://localhost:8815/payment/vnpay-callback";

    // ĐÂY LÀ BỘ MÃ CHUẨN - KHÔNG ĐƯỢC THAY ĐỔI
    public static String vnp_TmnCode = "LBRHRJD6";
    public static String vnp_HashSecret = "MAQJE5756WWKUA1BPTXNAJ71V510N6M7";

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null)
                throw new NullPointerException();
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            // ÉP BUỘC dùng UTF_8 để lấy Bytes từ Key và Data
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null)
                ipAdress = "127.0.0.1";
        } catch (Exception e) {
            ipAdress = "127.0.0.1";
        }
        return ipAdress;
    }
}
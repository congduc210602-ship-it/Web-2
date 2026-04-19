package com.rainbowforest.orderservice.controller;

import com.rainbowforest.orderservice.domain.Item;
import com.rainbowforest.orderservice.domain.Order;
import com.rainbowforest.orderservice.domain.User;
import com.rainbowforest.orderservice.feignclient.PaymentClient;
import com.rainbowforest.orderservice.feignclient.UserClient;
import com.rainbowforest.orderservice.http.header.HeaderGenerator;
import com.rainbowforest.orderservice.service.CartService;
import com.rainbowforest.orderservice.service.OrderService;
import com.rainbowforest.orderservice.utilities.OrderUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

@RestController
public class OrderController {

    @Autowired
    private UserClient userClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private HeaderGenerator headerGenerator;

    // Đã tiêm PaymentClient mới vào đây
    @Autowired
    private PaymentClient paymentClient;

    @PostMapping(value = "/order/{userId}")
    public ResponseEntity<?> saveOrder(
            @PathVariable("userId") Long userId,
            @RequestHeader(value = "Cookie") String cartId,
            HttpServletRequest request) {

        List<Item> cart = cartService.getAllItemsFromCart(cartId);
        User user = null;
        try {
            // Lấy thông tin User để gán vào Order
            user = userClient.getUserById(userId).getBody();
        } catch (Exception e) {
            System.out.println("Không tìm thấy User!");
        }

        if (cart != null && user != null && !cart.isEmpty()) {
            Order order = this.createOrder(cart, user);
            try {
                // 1. Lưu đơn hàng tạm để lấy Order ID
                orderService.saveOrder(order);

                // 2. Gọi thẳng sang PAYMENT SERVICE để xử lý thanh toán
                try {
                    ResponseEntity<String> paymentResponse = paymentClient.processPayment(
                            userId,
                            order.getId(),
                            order.getTotal());

                    // Nếu Payment Service xử lý thành công (Trả về 200 OK)
                    if (paymentResponse.getStatusCode() == HttpStatus.OK) {
                        order.setStatus("PAID"); // Đổi trạng thái thành Đã thanh toán
                        orderService.saveOrder(order); // Lưu cập nhật lại vào DB
                        cartService.deleteCart(cartId); // Xóa giỏ hàng

                        return new ResponseEntity<Order>(
                                order,
                                headerGenerator.getHeadersForSuccessPostMethod(request, order.getId()),
                                HttpStatus.CREATED);
                    }
                } catch (feign.FeignException e) {
                    // Nếu Payment Service báo lỗi (không đủ tiền sẽ quăng lỗi 400 Bad Request)
                    order.setStatus("PAYMENT_FAILED"); // Cập nhật đơn thành Thanh toán thất bại
                    orderService.saveOrder(order);

                    // Trả về thông báo lỗi cho người dùng
                    return new ResponseEntity<String>(
                            "Thanh toán thất bại! Số dư trong tài khoản không đủ.",
                            HttpStatus.BAD_REQUEST);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return new ResponseEntity<Order>(
                        headerGenerator.getHeadersForError(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<Order>(
                headerGenerator.getHeadersForError(),
                HttpStatus.NOT_FOUND);
    }

    // === API MỚI DÀNH CHO REACT CHECKOUT ===
    @PostMapping(value = "/order/checkout/{userId}")
    public ResponseEntity<?> placeOrderFromReact(
            @PathVariable("userId") Long userId,
            @RequestBody Order orderRequest,
            HttpServletRequest request) {
        try {
            User user = userClient.getUserById(userId).getBody();
            if (user == null)
                return new ResponseEntity<>("Không tìm thấy User!", HttpStatus.NOT_FOUND);

            orderRequest.setUser(user);
            orderRequest.setOrderedDate(LocalDate.now());

            // Xử lý Status dựa trên Payment Method
            if ("VNPAY".equals(orderRequest.getPaymentMethod())) {
                orderRequest.setStatus("Đang chờ thanh toán");
            } else {
                orderRequest.setStatus("Đang xử lý"); // COD
            }

            if (orderRequest.getItems() != null) {
                orderRequest.setTotal(OrderUtilities.countTotalPrice(orderRequest.getItems()));
            }

            orderService.saveOrder(orderRequest);

            return new ResponseEntity<>(orderRequest, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Order createOrder(List<Item> cart, User user) {
        Order order = new Order();
        order.setItems(cart);
        order.setUser(user);
        order.setTotal(OrderUtilities.countTotalPrice(cart));
        order.setOrderedDate(LocalDate.now());
        order.setStatus("PAYMENT_EXPECTED");
        return order;
    }

    // ==========================================
    // CÁC API DÀNH CHO ADMIN QUẢN LÝ ĐƠN HÀNG
    // ==========================================

    @GetMapping(value = "/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        if (!orders.isEmpty()) {
            return new ResponseEntity<List<Order>>(
                    orders,
                    headerGenerator.getHeadersForSuccessGetMethod(),
                    HttpStatus.OK);
        }
        return new ResponseEntity<List<Order>>(
                headerGenerator.getHeadersForError(),
                HttpStatus.NOT_FOUND);
    }

    @GetMapping(value = "/orders/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable("id") Long id) {
        Order order = orderService.getOrderById(id);
        if (order != null) {
            return new ResponseEntity<Order>(
                    order,
                    headerGenerator.getHeadersForSuccessGetMethod(),
                    HttpStatus.OK);
        }
        return new ResponseEntity<Order>(
                headerGenerator.getHeadersForError(),
                HttpStatus.NOT_FOUND);
    }

    @PostMapping(value = "/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            HttpServletRequest request) {

        Order order = orderService.getOrderById(id);
        if (order != null) {
            try {
                order.setStatus(status);
                orderService.saveOrder(order);
                return new ResponseEntity<Order>(
                        order,
                        headerGenerator.getHeadersForSuccessPostMethod(request, order.getId()),
                        HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<Order>(
                        headerGenerator.getHeadersForError(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<Order>(
                headerGenerator.getHeadersForError(),
                HttpStatus.NOT_FOUND);
    }

    // === API THỐNG KÊ CHO DASHBOARD ===

    @GetMapping(value = "/orders/dashboard/count")
    public ResponseEntity<Long> getOrdersCount() {
        try {
            long count = orderService.getAllOrders().size();
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(0L, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/orders/dashboard/revenue")
    public ResponseEntity<BigDecimal> getTotalRevenue() {
        try {
            List<Order> orders = orderService.getAllOrders();
            BigDecimal totalRevenue = BigDecimal.ZERO;
            for (Order order : orders) {
                if ("PAID".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus())) {
                    if (order.getTotal() != null) {
                        totalRevenue = totalRevenue.add(order.getTotal());
                    }
                }
            }
            return new ResponseEntity<>(totalRevenue, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(BigDecimal.ZERO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
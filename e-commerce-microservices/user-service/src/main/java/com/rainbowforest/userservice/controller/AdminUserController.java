package com.rainbowforest.userservice.controller;

import com.rainbowforest.userservice.entity.User;
import com.rainbowforest.userservice.entity.UserDetails;
import com.rainbowforest.userservice.http.header.HeaderGenerator;
import com.rainbowforest.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/admin/users") // Đặt đường dẫn riêng cho API Admin
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private HeaderGenerator headerGenerator;

    // === 1. LẤY TOÀN BỘ DANH SÁCH NGƯỜI DÙNG ===
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        if (users != null && !users.isEmpty()) {
            return new ResponseEntity<>(users, headerGenerator.getHeadersForSuccessGetMethod(), HttpStatus.OK);
        }
        return new ResponseEntity<>(headerGenerator.getHeadersForError(), HttpStatus.NOT_FOUND);
    }

    // === 2. THÊM NGƯỜI DÙNG MỚI TỪ ADMIN ===
    @PostMapping
    public ResponseEntity<User> addUserByAdmin(@RequestBody User user, HttpServletRequest request) {
        if (user != null) {
            try {
                // Ràng buộc 2 chiều giữa User và UserDetails để JPA lưu trữ đúng
                if (user.getUserDetails() != null) {
                    user.getUserDetails().setUser(user);
                }
                userService.saveUser(user);
                return new ResponseEntity<>(user, headerGenerator.getHeadersForSuccessPostMethod(request, user.getId()),
                        HttpStatus.CREATED);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    // === 3. CẬP NHẬT THÔNG TIN NGƯỜI DÙNG ===
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUserByAdmin(@PathVariable("id") Long id, @RequestBody User updatedUser,
            HttpServletRequest request) {
        User existingUser = userService.getUserById(id);
        if (existingUser != null) {
            try {
                // Cập nhật thông tin cơ bản
                existingUser.setUserName(updatedUser.getUserName());
                if (updatedUser.getUserPassword() != null && !updatedUser.getUserPassword().isEmpty()) {
                    existingUser.setUserPassword(updatedUser.getUserPassword());
                }
                existingUser.setActive(updatedUser.getActive());

                // Cập nhật UserDetails
                if (updatedUser.getUserDetails() != null) {
                    UserDetails newDetails = updatedUser.getUserDetails();
                    UserDetails currentDetails = existingUser.getUserDetails();

                    if (currentDetails == null) {
                        currentDetails = new UserDetails();
                        existingUser.setUserDetails(currentDetails);
                    }

                    currentDetails.setFirstName(newDetails.getFirstName());
                    currentDetails.setLastName(newDetails.getLastName());
                    currentDetails.setEmail(newDetails.getEmail());
                    currentDetails.setPhoneNumber(newDetails.getPhoneNumber());
                    // ... (Thêm các trường khác nếu cần thiết trên UI)
                    currentDetails.setUser(existingUser);
                }

                // Cập nhật Role
                if (updatedUser.getRole() != null) {
                    existingUser.setRole(updatedUser.getRole());
                }

                userService.saveUser(existingUser);
                return new ResponseEntity<>(existingUser,
                        headerGenerator.getHeadersForSuccessPostMethod(request, existingUser.getId()), HttpStatus.OK);

            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // === 4. KHÓA / MỞ KHÓA TÀI KHOẢN (ACTIVE STATUS) ===
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable("id") Long id,
            @RequestParam("active") int activeStatus) {
        User user = userService.getUserById(id);
        if (user != null) {
            try {
                user.setActive(activeStatus);
                userService.saveUser(user);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/dashboard/count")
    public ResponseEntity<Long> getTotalUsersCount() {
        try {
            long count = userService.getAllUsers().size();
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(0L, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
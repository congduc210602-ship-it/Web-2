package com.rainbowforest.userservice.service;

import com.rainbowforest.userservice.entity.User;
import com.rainbowforest.userservice.entity.UserRole;
import com.rainbowforest.userservice.repository.UserRepository;
import com.rainbowforest.userservice.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.getOne(id);
    }

    @Override
    public User getUserByName(String userName) {
        return userRepository.findByUserName(userName);
    }

    @Override
    public User saveUser(User user) {
        user.setActive(1);
        UserRole role = userRoleRepository.findUserRoleByRoleName("ROLE_USER");
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    public boolean deductBalance(Long userId, java.math.BigDecimal amount) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false; 

        // Bơm tiền test cho tài khoản cũ bị NULL
        if (user.getBalance() == null) {
            user.setBalance(new java.math.BigDecimal("100000000"));
        }

        // Kiểm tra số dư
        if (user.getBalance().compareTo(amount) < 0) {
            return false; 
        }

        // Chỉ trừ tiền và lưu User, KHÔNG LƯU Payment ở đây nữa
        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);

        return true;
    }
}

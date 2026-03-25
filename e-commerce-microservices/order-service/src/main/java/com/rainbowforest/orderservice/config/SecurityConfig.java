package com.rainbowforest.orderservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // Tắt tính năng chặn POST/DELETE mặc định
            .authorizeRequests()
            .anyRequest().permitAll(); // Mở khóa cho phép tất cả API đi qua không cần đăng nhập
    }
}
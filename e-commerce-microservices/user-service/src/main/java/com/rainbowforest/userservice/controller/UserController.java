package com.rainbowforest.userservice.controller;

import com.rainbowforest.userservice.entity.User;
import com.rainbowforest.userservice.http.header.HeaderGenerator;
import com.rainbowforest.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private HeaderGenerator headerGenerator;

	@Autowired
	private com.rainbowforest.userservice.security.JwtUtils jwtUtils;

	@GetMapping(value = "/users")
	public ResponseEntity<List<User>> getAllUsers() {
		List<User> users = userService.getAllUsers();
		if (!users.isEmpty()) {
			return new ResponseEntity<List<User>>(
					users,
					headerGenerator.getHeadersForSuccessGetMethod(),
					HttpStatus.OK);
		}
		return new ResponseEntity<List<User>>(
				headerGenerator.getHeadersForError(),
				HttpStatus.NOT_FOUND);
	}

	@GetMapping(value = "/users", params = "name")
	public ResponseEntity<User> getUserByName(@RequestParam("name") String userName) {
		User user = userService.getUserByName(userName);
		if (user != null) {
			return new ResponseEntity<User>(
					user,
					headerGenerator.getHeadersForSuccessGetMethod(),
					HttpStatus.OK);
		}
		return new ResponseEntity<User>(
				headerGenerator.getHeadersForError(),
				HttpStatus.NOT_FOUND);
	}

	@GetMapping(value = "/users/{id}")
	public ResponseEntity<User> getUserById(@PathVariable("id") Long id) {
		User user = userService.getUserById(id);
		if (user != null) {
			return new ResponseEntity<User>(
					user,
					headerGenerator.getHeadersForSuccessGetMethod(),
					HttpStatus.OK);
		}
		return new ResponseEntity<User>(
				headerGenerator.getHeadersForError(),
				HttpStatus.NOT_FOUND);
	}

	@PostMapping(value = "/users")
	public ResponseEntity<User> addUser(@RequestBody User user, HttpServletRequest request) {
		if (user != null)
			try {
				userService.saveUser(user);
				return new ResponseEntity<User>(
						user,
						headerGenerator.getHeadersForSuccessPostMethod(request, user.getId()),
						HttpStatus.CREATED);
			} catch (Exception e) {
				e.printStackTrace();
				return new ResponseEntity<User>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		return new ResponseEntity<User>(HttpStatus.BAD_REQUEST);
	}

	@PostMapping("/users/{id}/deduct-balance")
	public org.springframework.http.ResponseEntity<String> deductBalance(
			@PathVariable("id") Long id,
			@RequestParam("amount") java.math.BigDecimal amount) {

		boolean isSuccess = userService.deductBalance(id, amount);
		if (isSuccess) {
			return new org.springframework.http.ResponseEntity<>("SUCCESS", org.springframework.http.HttpStatus.OK);
		} else {
			return new org.springframework.http.ResponseEntity<>("INSUFFICIENT_BALANCE",
					org.springframework.http.HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody User loginRequest) {
		User user = userService.checkLogin(loginRequest.getUserName(), loginRequest.getUserPassword());

		if (user != null) {
			// Khách hàng hoặc Admin đều đăng nhập thành công
			String token = jwtUtils.generateToken(user.getUserName());

			// Lấy role của user (nếu không có thì mặc định là USER)
			String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "USER";

			java.util.Map<String, Object> response = new java.util.HashMap<>();
			response.put("token", token);
			response.put("userName", user.getUserName());
			response.put("role", roleName); // Trả về vai trò thật của họ

			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		return new ResponseEntity<>("Sai tài khoản hoặc mật khẩu!", HttpStatus.UNAUTHORIZED);
	}
}

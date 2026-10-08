package com.smartapi.controller;

import com.smartapi.dto.AuthDtos.AuthResponse;
import com.smartapi.dto.AuthDtos.LoginRequest;
import com.smartapi.dto.AuthDtos.RegisterRequest;
import com.smartapi.dto.AuthDtos.UserResponse;
import com.smartapi.security.AuthInterceptor;
import com.smartapi.security.CurrentUser;
import com.smartapi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.name(), request.email(), request.password());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.me(CurrentUser.id());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        authService.logout(AuthInterceptor.bearerToken(request));
    }
}

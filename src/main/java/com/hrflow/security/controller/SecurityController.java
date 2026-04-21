package com.hrflow.security.controller;

import com.hrflow.security.dtos.LoginRequest;
import com.hrflow.security.dtos.LoginResponse;
import com.hrflow.security.dtos.RefreshTokenRequest;
import com.hrflow.security.services.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class SecurityController {

    private final AuthenticationService authenticationService;

    public SecurityController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request.refreshToken()));
    }


}
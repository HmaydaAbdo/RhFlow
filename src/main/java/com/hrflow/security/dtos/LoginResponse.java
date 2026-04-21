package com.hrflow.security.dtos;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String email,
        String fullName,
        List<String> roles
) {}

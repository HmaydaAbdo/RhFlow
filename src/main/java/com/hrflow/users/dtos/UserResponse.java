package com.hrflow.users.dtos;

import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String gsm,
        boolean enabled,
        List<String> roles
) {}

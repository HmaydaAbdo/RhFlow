package com.hrflow.users.mapper;

import com.hrflow.users.dtos.UserResponse;
import com.hrflow.roles.entities.Role;
import com.hrflow.users.entities.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserResponse toDto(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .toList();

        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getGsm(), user.isEnabled(), roles);
    }
}

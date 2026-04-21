package com.hrflow.users.exception;

public class UserRoleException extends RuntimeException {

    public UserRoleException(String message) {
        super(message);
    }

    public static UserRoleException alreadyAssigned(Long userId, Long roleId) {
        return new UserRoleException("User " + userId + " already has role " + roleId);
    }

    public static UserRoleException notAssigned(Long userId, Long roleId) {
        return new UserRoleException("User " + userId + " does not have role " + roleId);
    }
}
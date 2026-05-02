package com.hrflow.roles.exception;


public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(Long id) {
        super("Role not found with id: " + id);
    }
}
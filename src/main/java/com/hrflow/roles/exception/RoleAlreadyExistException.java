package com.hrflow.roles.exception;


public class RoleAlreadyExistException extends RuntimeException {

    public RoleAlreadyExistException(String roleName) {
        super("Role already exists: " + roleName);
    }
}
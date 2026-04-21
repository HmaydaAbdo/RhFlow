package com.hrflow.users.exception;

public class UserAlreadyExistException extends RuntimeException {

    public UserAlreadyExistException(String username) {

        super("Username already exists: " + username);
    }
}
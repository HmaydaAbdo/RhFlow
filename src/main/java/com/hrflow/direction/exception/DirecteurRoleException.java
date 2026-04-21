// src/main/java/com/hrflow/recruitment/direction/exception/DirecteurRoleException.java
package com.hrflow.direction.exception;

public class DirecteurRoleException extends RuntimeException {
    public DirecteurRoleException(Long userId) {
        super("L'utilisateur " + userId + " n'a pas le rôle DIRECTEUR");
    }
}

package com.hrflow.shared.dtos;

import java.util.Map;

public class ValidationErrorResponse extends ErrorResponse {

    private Map<String, String> fieldErrors;

    public ValidationErrorResponse(Map<String, String> fieldErrors) {
        super(400, "Validation Failed", "One or more fields are invalid");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
}
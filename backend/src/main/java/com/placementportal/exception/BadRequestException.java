package com.placementportal.exception;

/** Maps to HTTP 400: the request was syntactically fine but breaks a business rule. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}

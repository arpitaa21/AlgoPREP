package com.placementportal.exception;

/** Maps to HTTP 409: the request clashes with the current state (duplicate email, already submitted ...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}

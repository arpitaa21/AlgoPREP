package com.placementportal.exception;

/** Thrown when a test is submitted after deadline + grace period. Maps to HTTP 409. */
public class TestExpiredException extends RuntimeException {
    public TestExpiredException(String message) { super(message); }
}

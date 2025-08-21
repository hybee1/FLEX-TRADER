package com.synogiestechnologies.flex_trader_auth.Exceptions;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends RuntimeException{
    public HttpStatus httpStatus;
    public InvalidCredentialsException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public InvalidCredentialsException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}

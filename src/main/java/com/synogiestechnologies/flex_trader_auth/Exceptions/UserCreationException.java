package com.synogiestechnologies.flex_trader_auth.Exceptions;

import org.springframework.http.HttpStatus;

public class UserCreationException extends RuntimeException{

    public HttpStatus httpStatus;

    public UserCreationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public UserCreationException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}

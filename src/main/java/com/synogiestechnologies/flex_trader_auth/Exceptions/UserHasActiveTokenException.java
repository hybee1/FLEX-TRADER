package com.synogiestechnologies.flex_trader_auth.Exceptions;


import org.springframework.http.HttpStatus;


public class UserHasActiveTokenException extends RuntimeException{

    public HttpStatus httpStatus;

    public UserHasActiveTokenException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public UserHasActiveTokenException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}




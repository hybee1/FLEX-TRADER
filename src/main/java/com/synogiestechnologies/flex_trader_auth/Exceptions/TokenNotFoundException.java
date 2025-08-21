package com.synogiestechnologies.flex_trader_auth.Exceptions;

import org.springframework.http.HttpStatus;


public class TokenNotFoundException extends RuntimeException{

    public HttpStatus httpStatus;

    public TokenNotFoundException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public TokenNotFoundException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}




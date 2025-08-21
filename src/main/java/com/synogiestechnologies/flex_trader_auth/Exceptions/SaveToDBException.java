package com.synogiestechnologies.flex_trader_auth.Exceptions;

import org.springframework.http.HttpStatus;


public class SaveToDBException extends RuntimeException{

    public HttpStatus httpStatus;

    public SaveToDBException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public SaveToDBException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}




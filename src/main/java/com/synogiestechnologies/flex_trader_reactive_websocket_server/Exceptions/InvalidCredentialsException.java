package com.synogiestechnologies.flex_trader_reactive_websocket_server.Exceptions;

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

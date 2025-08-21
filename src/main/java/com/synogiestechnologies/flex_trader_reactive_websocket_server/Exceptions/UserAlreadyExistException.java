package com.synogiestechnologies.flex_trader_reactive_websocket_server.Exceptions;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistException extends RuntimeException{
    public HttpStatus httpStatus;
    public UserAlreadyExistException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public UserAlreadyExistException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}

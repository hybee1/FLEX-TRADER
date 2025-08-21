package com.synogiestechnologies.flex_trader_websocket_server.Exceptions;

import org.springframework.http.HttpStatus;

public class NoUserFoundException extends RuntimeException{

    public HttpStatus httpStatus;

    public NoUserFoundException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public NoUserFoundException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}

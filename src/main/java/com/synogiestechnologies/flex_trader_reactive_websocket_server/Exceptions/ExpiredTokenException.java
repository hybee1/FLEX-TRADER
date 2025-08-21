package com.synogiestechnologies.flex_trader_reactive_websocket_server.Exceptions;//package com.synogiestechnologies.flex_trader_auth.exception;

import org.springframework.http.HttpStatus;


public class ExpiredTokenException extends RuntimeException{

    public HttpStatus httpStatus;

    public ExpiredTokenException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public ExpiredTokenException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}




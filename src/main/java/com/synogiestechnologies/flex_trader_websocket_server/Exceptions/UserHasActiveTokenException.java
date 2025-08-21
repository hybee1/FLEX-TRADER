package com.synogiestechnologies.flex_trader_websocket_server.Exceptions;//package com.synogiestechnologies.flex_trader_auth.exception;

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




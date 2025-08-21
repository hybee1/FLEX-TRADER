package com.synogiestechnologies.flex_trader_websocket_server.Exceptions;//package com.synogiestechnologies.flex_trader_auth.exception;

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




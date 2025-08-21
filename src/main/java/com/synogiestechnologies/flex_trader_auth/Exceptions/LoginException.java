package com.synogiestechnologies.flex_trader_auth.Exceptions;
import org.springframework.http.HttpStatus;


public class LoginException extends RuntimeException{

    public HttpStatus httpStatus;

    public LoginException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public LoginException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}




package com.synogiestechnologies.flex_trader_websocket_server.Exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
//@AllArgsConstructor
@RequiredArgsConstructor
//@NoArgsConstructor
public class MyUsersException {

    //private final String message;
    private final Map<String,Object> message;
    private final Throwable throwable;

}

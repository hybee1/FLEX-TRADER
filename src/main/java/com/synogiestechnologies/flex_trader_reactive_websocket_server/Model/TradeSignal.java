package com.synogiestechnologies.flex_trader_reactive_websocket_server.Model;


import java.time.LocalDateTime;


public class TradeSignal {
    private String symbol;
    private double price;
    private String side; // "BUY" or "SELL"
    private LocalDateTime date;

    // Constructors, getters, setters, toString()
}


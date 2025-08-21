package com.synogiestechnologies.flex_trader_reactive_websocket_server.DTOResponse;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotSignalMessage {
    private Map<String, Object> signal;
}
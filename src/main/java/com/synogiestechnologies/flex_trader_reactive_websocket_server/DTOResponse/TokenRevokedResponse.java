package com.synogiestechnologies.flex_trader_reactive_websocket_server.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenRevokedResponse {

    private DataObj data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataObj {
        private boolean revoked;
    }
}

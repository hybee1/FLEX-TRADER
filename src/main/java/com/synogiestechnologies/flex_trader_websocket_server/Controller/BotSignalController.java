package com.synogiestechnologies.flex_trader_websocket_server.Controller;

import com.synogiestechnologies.flex_trader_websocket_server.DTOResponse.BotSignalMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class BotSignalController {

    private final SimpMessagingTemplate template;

    public BotSignalController(SimpMessagingTemplate template) {
        this.template = template;
    }


    // if you use @PreAuthorize("principal.name == 'alice'") here remember to remove the
    // StompCommand.SEND.equals(accessor.getCommand() in configureClientInboundChannel->
    // configureClientInboundChannel-> preSend
    // @PreAuthorize("principal.name == 'alice'")
    @MessageMapping("/to-server")
    // Only “alice” may call /app/chat.send
//    @PreAuthorize("principal.name == 'alice'")
    public void sendMessage(BotSignalMessage msg, Principal principal) {
        // 1) Received only if authenticated user’s name == "alice"
        // 2) Now broadcast to everyone subscribed to /topic/messages
        template.convertAndSend("/topic/broadcast/bot-signal", msg);
    }
}


package com.synogiestechnologies.flex_trader_websocket_server.RabbitMQConsumer;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


@Component
public class SignalListener {

    private final SimpMessagingTemplate messagingTemplate;

    public SignalListener(SimpMessagingTemplate messagingTemplate) {

        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = "${RABBITMQ_QUEUE_NAME_1}")
    public void receiveMessage(String message) {
        messagingTemplate.convertAndSend("/topic/broadcast/bot-signal", message);
    }
}

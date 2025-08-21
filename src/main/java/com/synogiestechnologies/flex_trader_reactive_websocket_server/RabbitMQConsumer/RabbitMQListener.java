package com.synogiestechnologies.flex_trader_reactive_websocket_server.RabbitMQConsumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.synogiestechnologies.flex_trader_reactive_websocket_server.DTOResponse.BotSignalMessage;
import com.synogiestechnologies.flex_trader_reactive_websocket_server.TradeEventPublisher.TradeEventPublisher;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.rabbitmq.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


@Component
public class RabbitMQListener {

    @Value("${RABBITMQ_EXCHANGE_NAME_2}")
    private String exchangeName;

    @Value("${RABBITMQ_EXCHANGE_TYPE_NAME_2}")
    private String exchangeType;

    @Value("${RABBITMQ_QUEUE_NAME_2}")
    private String queueName;

    private final Receiver receiver;
    private final Sender sender;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TradeEventPublisher eventPublisher;

    public RabbitMQListener(Receiver receiver, Sender sender,
                            TradeEventPublisher eventPublisher) {

        System.out.println("INSIDE RabbitMQListener");

        this.receiver = receiver;
        this.sender = sender;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void startConsuming() {
//        String exchangeName = "trade-exchange-fanout";
        queueName = queueName + "-" + UUID.randomUUID(); // unique per instance

        System.out.println("INSIDE PostConstruct ");

        sender.declareExchange(ExchangeSpecification
                        .exchange(exchangeName).type(exchangeType)
                        .durable(true))

                .then(sender.declareQueue(QueueSpecification
                        .queue(queueName).durable(false)))

                .then(sender.bind(BindingSpecification
                        .binding(exchangeName,"", queueName)))
                .block();

        receiver.consumeAutoAck(queueName)
                .map(delivery -> {
                    byte[] body = delivery.getBody();
                    String rawJson = new String(body, StandardCharsets.UTF_8);
                    System.out.println("Raw message body: " + rawJson);
                    try {
                        System.out.println("delivery = " + delivery.getBody().getClass());
                        System.out.println("delivery = " + delivery.getClass().getName());
                        return objectMapper.readValue(delivery.getBody(),
                                BotSignalMessage.class);
                    } catch (IOException e) {
                        throw new RuntimeException("Invalid message format", e);
                    }
                })
                .doOnNext(eventPublisher::emit) // push to all WebSocket subscribers in this instance
                .doOnError(e -> System.err.println("Rabbit stream error: " + e.getMessage()))
                .subscribe(msg -> System.out.println("🟢 Received: " + msg));
    }


}

//package com.synogiestechnologies.flex_trader_reactive_websocket_server.Config;
//
//
//import com.rabbitmq.client.*;
//import reactor.rabbitmq.*;
//
//
//public class RabbitMqFanOutConfig {
//
//    private final Sender sender;
//    private final Receiver receiver;
//
//    public RabbitMqFanOutConfig() {
//        ConnectionFactory connectionFactory = new ConnectionFactory();
//        connectionFactory.setHost("localhost");
//
//        SenderOptions senderOptions = new SenderOptions()
//                .connectionFactory(connectionFactory);
//
//        ReceiverOptions receiverOptions = new ReceiverOptions()
//                .connectionFactory(connectionFactory);
//
//        this.sender = RabbitFlux.createSender(senderOptions);
//        this.receiver = RabbitFlux.createReceiver(receiverOptions);
//    }
//
//    public void setup() {
//        String exchangeName = "trade-fanout-exchange";
//        String queueName = "trade-fanout-queue";
//
//        // Declare exchange & queue and bind
//        sender.declareExchange(ExchangeSpecification.exchange(exchangeName)
//                        .type(BuiltinExchangeType.FANOUT.getType())
//                        .durable(true))
//                .then(sender.declareQueue(QueueSpecification.queue(queueName).durable(true)))
//                .then(sender.bind(BindingSpecification.binding(exchangeName, "", queueName)))
//                .block();
//    }
//
//    // @PostConstruct
//    public void consume() {
//        receiver.consumeAutoAck("trade-fanout-queue")
//                .map(Delivery::getBody)
//                .map(String::new)
//                .subscribe(msg -> System.out.println("Received: " + msg));
//    }
//}

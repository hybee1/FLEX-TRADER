package com.synogiestechnologies.flex_trader_reactive_websocket_server.Config;


import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;

@Configuration
public class RabbitMQConfig {

//    @Value("${RABBITMQ_QUEUE_NAME_2}")
//    private static String QUEUE_NAME;

    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // Change if RabbitMQ is remote
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory;
    }


    @Bean
    public Sender sender(ConnectionFactory connectionFactory) {
        return RabbitFlux.createSender(new SenderOptions()
                .connectionFactory(connectionFactory)
                .resourceManagementScheduler(Schedulers.boundedElastic()));
    }

    @Bean
    public Receiver receiver(ConnectionFactory connectionFactory) {
        return RabbitFlux.createReceiver(new ReceiverOptions()
                .connectionFactory(connectionFactory)
                .connectionSubscriptionScheduler(Schedulers.boundedElastic()));
    }

    @Bean
    public QueueSpecification queueSpecification(@Value("${RABBITMQ_QUEUE_NAME_2}") String QUEUE_NAME) {

        return QueueSpecification.queue(QUEUE_NAME);
    }
}

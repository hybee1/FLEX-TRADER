package com.synogiestechnologies.flex_trader_websocket_server.Config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

//    @Value("${RABBITMQ_QUEUE_NAME_1}")
//    private String queueName;
//
//    @Value("${RABBITMQ_EXCHANGE_NAME_1}")
//    private String exchangeName;
//
//    @Value("${RABBITMQ_ROUTING_KEY_1}")
//    private String routingKey;

//    public RabbitMQConfig(String queueName, String exchangeName, String routingKey) {
//        this.queueName = queueName;
//        this.exchangeName = exchangeName;
//        this.routingKey = routingKey;
//    }


    @Bean
    public Queue queue(@Value("${RABBITMQ_QUEUE_NAME_1}") String queueName) {

        return new Queue(queueName, true);
    }

//    @Bean
//    public Queue queue() {
//
//        return new Queue(queueName, true);
//    }

    @Bean
    public DirectExchange exchange(@Value("${RABBITMQ_EXCHANGE_NAME_1}") String exchangeName) {
        return new DirectExchange(exchangeName);
    }

//    @Bean
//    public DirectExchange exchange() {
//        return new DirectExchange(exchangeName);
//    }

    @Bean
    public Binding binding( Queue queue,  DirectExchange exchange,
                            @Value("${RABBITMQ_ROUTING_KEY_1}") String routingKey ) {

        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

//    @Bean
//    public Binding binding( Queue queue,  DirectExchange exchange, String routingKey ) {
//
//        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
//    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }


}

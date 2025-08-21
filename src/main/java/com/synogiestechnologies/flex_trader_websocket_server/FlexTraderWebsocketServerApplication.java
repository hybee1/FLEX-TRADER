package com.synogiestechnologies.flex_trader_websocket_server;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class FlexTraderWebsocketServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlexTraderWebsocketServerApplication.class, args);
	}

//	@Bean
//	public CommandLineRunner runner(Queue queue, DirectExchange exchange, Binding binding) {
//		return args -> {
//			System.out.println("Queue: " + queue.getName());
//			System.out.println("Exchange: " + exchange.getName());
//			System.out.println("Binding with key: " + binding.getRoutingKey());
//		};
//	}

	@Bean
	public CommandLineRunner checkProps(@Value("${RABBITMQ_QUEUE_NAME_1}") String queueName) {
		return args -> {
			System.out.println("Loaded queue name: " + queueName);
		};
	}

}

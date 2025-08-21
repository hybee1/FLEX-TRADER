package com.synogiestechnologies.flex_trader_api_gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class FlexTraderApiGatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlexTraderApiGatewayServiceApplication.class, args);
	}

}

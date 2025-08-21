package com.synogiestechnologies.flex_trader_discovery_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class FlexTraderDiscoveryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlexTraderDiscoveryServiceApplication.class, args);
	}

}

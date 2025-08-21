package com.synogiestechnologies.flex_trader_config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class FlexTraderConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlexTraderConfigServerApplication.class, args);
	}

}

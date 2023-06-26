package com.grpcroshambo.roshamboserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RoshamboServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoshamboServerApplication.class, args);
	}

}

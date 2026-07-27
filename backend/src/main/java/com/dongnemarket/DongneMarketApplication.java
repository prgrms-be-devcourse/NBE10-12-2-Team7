package com.dongnemarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DongneMarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(DongneMarketApplication.class, args);
	}

}

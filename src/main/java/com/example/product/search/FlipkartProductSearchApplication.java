package com.example.product.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableCaching
@EnableKafka
@SpringBootApplication
public class FlipkartProductSearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlipkartProductSearchApplication.class, args);
	}

}

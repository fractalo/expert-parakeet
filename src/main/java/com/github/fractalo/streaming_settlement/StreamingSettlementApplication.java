package com.github.fractalo.streaming_settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StreamingSettlementApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamingSettlementApplication.class, args);
	}

}

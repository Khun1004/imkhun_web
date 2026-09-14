package com.imkhun.imkhun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ImkhunApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImkhunApplication.class, args);
	}

}
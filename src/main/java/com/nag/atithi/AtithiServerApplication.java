package com.nag.atithi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AtithiServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtithiServerApplication.class, args);
	}

}


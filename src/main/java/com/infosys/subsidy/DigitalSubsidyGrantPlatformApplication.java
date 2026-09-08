package com.infosys.subsidy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DigitalSubsidyGrantPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalSubsidyGrantPlatformApplication.class, args);
	}

}

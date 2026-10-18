package com.projetofit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProjetofitApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjetofitApplication.class, args);
	}

}

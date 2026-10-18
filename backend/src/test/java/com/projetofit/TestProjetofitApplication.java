package com.projetofit;

import org.springframework.boot.SpringApplication;

public class TestProjetofitApplication {

	public static void main(String[] args) {
		SpringApplication.from(ProjetofitApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

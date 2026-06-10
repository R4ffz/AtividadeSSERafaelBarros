package br.com.rafaelbarros.sse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtividadeSseApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtividadeSseApplication.class, args);
	}

}

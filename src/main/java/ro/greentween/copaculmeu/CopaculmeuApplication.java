package ro.greentween.copaculmeu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ro.greentween.copaculmeu", "tech.petrepopescu"})
public class CopaculmeuApplication {

	static void main(String[] args) {
		SpringApplication.run(CopaculmeuApplication.class, args);
	}

}

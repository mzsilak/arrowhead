package eu.arrowhead.gams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class GamsMain {

    public GamsMain() {
        super();
    }

    public static void main(String[] args) {
        SpringApplication.run(GamsMain.class, args);
    }
}

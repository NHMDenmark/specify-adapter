package dk.northtech.dassco_specify_adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DasscoSpecifyAdapterApplication {

	public static void main(String[] args) {
		SpringApplication.run(DasscoSpecifyAdapterApplication.class, args);
	}

}

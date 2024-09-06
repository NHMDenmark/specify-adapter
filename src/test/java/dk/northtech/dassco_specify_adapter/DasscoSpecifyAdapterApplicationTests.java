package dk.northtech.dassco_specify_adapter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@DirtiesContext
@ActiveProfiles("tests")
class DasscoSpecifyAdapterApplicationTests {

	@Test
	void contextLoads() {
	}

}

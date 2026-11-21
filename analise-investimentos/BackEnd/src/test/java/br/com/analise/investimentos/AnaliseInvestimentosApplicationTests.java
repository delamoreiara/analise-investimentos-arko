package br.com.analise.investimentos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"databricks.host=https://localhost",
		"databricks.token=test-token",
		"databricks.warehouse-id=test-warehouse"
})
class AnaliseInvestimentosApplicationTests {

	@Test
	void contextLoads() {
	}

}

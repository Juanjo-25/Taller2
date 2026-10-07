package com.example.yate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:contexto")
class YateApplicationTests {

	@Test
	void contextLoads() {
	}

}

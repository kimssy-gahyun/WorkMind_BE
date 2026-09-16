package com.gh.workmind;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.gh.workmind.auth.JwtTestSupport;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WorkMindBeApplicationTests {

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        JwtTestSupport.configure(registry);
    }

	@Test
	void contextLoads() {
	}

}

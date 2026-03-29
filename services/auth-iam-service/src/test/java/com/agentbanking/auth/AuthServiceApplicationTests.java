package com.agentbanking.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
        // Note: This test requires a PostgreSQL database connection
        // For testing without a database, use @DataJpaTest or @WebMvcTest instead
    }
}
package com.kabooz.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptTest {

    @Test
    public void testPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String password = "kabooz@2024";
        String hashInDb = "$2a$10$hKDVYxLefVHV/vtuPhWD3OigtRyOykAGfhaGmXkUqIBJoQ7iIOgUG";

        System.out.println("========================================");
        System.out.println("Password: " + password);
        System.out.println("Hash in DB: " + hashInDb);
        System.out.println("Matches: " + encoder.matches(password, hashInDb));
        System.out.println("Generated Hash: " + encoder.encode(password));
        System.out.println("========================================");
    }
}

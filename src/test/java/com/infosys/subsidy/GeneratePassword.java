package com.infosys.subsidy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GeneratePassword {
    public static void main(String[] args) {
        System.out.println("HASH_START");
        System.out.println(new BCryptPasswordEncoder().encode("password123"));
        System.out.println("HASH_END");
    }
}

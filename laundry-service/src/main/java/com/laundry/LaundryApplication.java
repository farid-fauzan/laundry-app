package com.laundry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Laundry Service — Entry Point
 *
 * @SpringBootApplication enables:
 *   - @Configuration    : marks as bean source
 *   - @EnableAutoConfiguration : Spring IoC auto-wires beans
 *   - @ComponentScan    : scans com.laundry.* for @Component, @Service, @Repository
 */
@SpringBootApplication
public class LaundryApplication {
    public static void main(String[] args) {
        SpringApplication.run(LaundryApplication.class, args);
    }
}

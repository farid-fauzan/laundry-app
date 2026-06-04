package com.laundry.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger Configuration
 *
 * UI  : http://localhost:8080/swagger-ui.html
 * JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI laundryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Laundry Service API")
                        .description("""
                                REST API untuk sistem manajemen laundry.

                                **Fitur:**
                                - CRUD order laundry
                                - Laporan revenue & statistik
                                - Riwayat pelanggan
                                - Deteksi order terlambat
                                - Bulk status update

                                **Tech Stack:** Spring Boot 3 · PostgreSQL · Redis · Kafka
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Laundry Dev Team")
                                .email("dev@laundry.com"))
                        .license(new License()
                                .name("MIT License")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development"),
                        new Server()
                                .url("http://laundry-service:8080")
                                .description("Docker Internal")))
                .tags(List.of(
                        new Tag().name("Orders").description("CRUD & status management"),
                        new Tag().name("Reports").description("Revenue, statistik, & analitik"),
                        new Tag().name("Customers").description("Riwayat & data pelanggan")));
    }
}

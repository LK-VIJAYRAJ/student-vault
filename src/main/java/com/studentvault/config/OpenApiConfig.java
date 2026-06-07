package com.studentvault.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * Access the UI at: http://localhost:8080/swagger-ui.html
 * Raw JSON spec at: http://localhost:8080/v3/api-docs
 *
 * Interview talking point:
 *   "I self-document the API with OpenAPI 3.0 so any developer can explore
 *    and test all endpoints without reading source code."
 */
@Configuration
public class OpenApiConfig {

    // Set APP_BASE_URL env var in production to show the correct server in Swagger UI
    // e.g. on Koyeb: APP_BASE_URL=https://student-vault-xxx.koyeb.app
    @Value("${app.base-url:}")
    private String appBaseUrl;

    @Bean
    public OpenAPI studentVaultOpenAPI() {
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url("http://localhost:8080").description("Local / Docker"));
        servers.add(new Server().url("https://student-vault-production-e297.up.railway.app").description("Railway"));
        if (appBaseUrl != null && !appBaseUrl.isBlank()) {
            servers.add(0, new Server().url(appBaseUrl).description("Production (Koyeb)"));
        }

        return new OpenAPI()
                .info(new Info()
                        .title("StudentVault API")
                        .description("""
                                Production-grade **Student Result Management System** with live database \
                                query benchmarking.
                                
                                ### Key Feature
                                Hit **GET /api/benchmark/run** to see real before/after query performance \
                                on 50,000 students — B-tree and composite indexes measured live against PostgreSQL 16.
                                
                                ### Data
                                - **50,000 students** across 5 departments, seeded via Flyway V2
                                - **200,000 results** (4 per student on average)
                                - **3 Flyway migrations**: schema → seed → indexes
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LK-VIJAYRAJ")
                                .url("https://github.com/LK-VIJAYRAJ/student-vault"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(servers)
                .tags(List.of(
                        new Tag().name("Students").description("CRUD operations for student records"),
                        new Tag().name("Results").description("Exam results, toppers, and grade distribution"),
                        new Tag().name("Benchmark").description("⚡ Live query optimization benchmark — the standout feature")
                ));
    }
}

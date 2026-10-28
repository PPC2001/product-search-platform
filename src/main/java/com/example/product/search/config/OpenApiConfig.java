package com.example.product.search.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productSearchOpenApi(@Value("${server.port:8085}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Flipkart Product Search API")
                        .version("1.0.0")
                        .description("Product write API (Oracle + transactional outbox) and "
                                + "Elasticsearch-backed search API.")
                        .contact(new Contact().name("Product Search Team")))
                .servers(List.of(new Server()
                        .url("http://localhost:" + port)
                        .description("Local")));
    }
}

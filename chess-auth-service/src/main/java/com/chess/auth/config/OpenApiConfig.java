package com.chess.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Bean
    public OpenAPI chessAuthServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chess Auth Service API")
                        .description("Authentication and Authorization Service for Chess Platform")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Chess Platform Team")
                                .email("support@chess-platform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Server"),
                        new Server().url("https://api.chess-platform.com").description("Production Server")
                ));
    }
}


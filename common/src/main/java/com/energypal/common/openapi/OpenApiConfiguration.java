package com.energypal.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(OpenAPI.class)
public class OpenApiConfiguration {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI energyPalOpenApi(@Value("${spring.application.name:energypal-service}") String serviceName) {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .info(new Info()
                        .title(toTitle(serviceName))
                        .version("0.1.0")
                        .description("EnergyPal Java 17 Spring Boot API"));
    }

    private String toTitle(String serviceName) {
        var words = serviceName.replace('-', ' ').split("\\s+");
        var title = new StringBuilder("EnergyPal ");
        for (String word : words) {
            if (!word.isBlank()) {
                title.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(' ');
            }
        }
        return title.toString().trim();
    }
}

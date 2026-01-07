package com.utilityexplorer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
        title = "Utility Explorer API",
        version = "1.0.0",
        description = "REST API for metrics catalog, map/timeseries data, ingestion control, and status transparency.",
        contact = @Contact(name = "Utility Explorer Team")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local")
    }
)
@SecurityScheme(
    name = "X-API-Key",
    type = SecuritySchemeType.APIKEY,
    paramName = "X-API-Key",
    in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER
)
@Configuration
public class OpenApiConfig {
}

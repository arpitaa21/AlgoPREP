package com.placementportal.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(Environment environment) {
        String pgHost = environment.getProperty("DB_HOST");
        String url;

        if (pgHost != null && !pgHost.isBlank()) {
            String port = environment.getProperty("DB_PORT", "5432");
            String database = environment.getProperty("DB_NAME", "placement_portal");
            url = "jdbc:postgresql://" + pgHost + ":" + port + "/" + database;
        } else {
            url = environment.getRequiredProperty("spring.datasource.url");
        }

        return DataSourceBuilder.create()
            .url(url)
            .username(environment.getProperty("DB_USER", "root"))
            .password(environment.getProperty("DB_PASSWORD", "root"))
            .build();
    }
}

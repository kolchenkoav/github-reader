package com.example.githubreader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для настроек директории.
 */
@Configuration
@ConfigurationProperties(prefix = "directory")
@Data
public class DirectoryConfig {
    /**
     * Путь к директории по умолчанию.
     */
    private String defaultPath;
}
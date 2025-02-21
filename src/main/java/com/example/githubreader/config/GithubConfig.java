package com.example.githubreader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурационный класс для настройки параметров GitHub.
 */
@Configuration
@ConfigurationProperties(prefix = "github")
@Data
public class GithubConfig {
    /**
     * Токен для доступа к GitHub API.
     */
    private String token;

    /**
     * Список шаблонов для включения репозиториев.
     */
    private List<String> includePatterns;

    /**
     * Список шаблонов для исключения репозиториев.
     */
    private List<String> excludePatterns;

    /**
     * Путь к файлу для сохранения содержимого одного файла.
     */
    private String singleFilePath = "output/all_contents.txt";
}
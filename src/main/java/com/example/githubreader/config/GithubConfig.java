package com.example.githubreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурационный класс для настройки параметров GitHub.
 */
@Configuration
@ConfigurationProperties(prefix = "github")
public class GithubConfig {
    public GithubConfig() {
    }

    public GithubConfig(List<String> includePatterns, List<String> excludePatterns) {
        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;
    }

    /**
     * Список шаблонов для включения репозиториев.
     */
    private List<String> includePatterns;

    /**
     * Список шаблонов для исключения репозиториев.
     */
    private List<String> excludePatterns;



    public List<String> getIncludePatterns() {
        return includePatterns;
    }

    public void setIncludePatterns(List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

}
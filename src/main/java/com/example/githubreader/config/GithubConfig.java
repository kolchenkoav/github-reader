package com.example.githubreader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "github")
@Data
public class GithubConfig {
    private String token;
    private List<String> includePatterns;
    private List<String> excludePatterns;
    private String singleFilePath = "output/all_contents.txt";
}
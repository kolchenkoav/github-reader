package com.example.githubreader.service;

import com.example.githubreader.config.GithubConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubContentServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GithubConfig githubConfig;

    @InjectMocks
    private GithubContentService githubContentService;

    private HttpEntity<String> httpEntity;

    @BeforeEach
    void setUp() {
        when(githubConfig.getToken()).thenReturn("test-token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer test-token");
        headers.set("Accept", "application/vnd.github.v3+json");
        httpEntity = new HttpEntity<>(headers);

        // Мокаем паттерны включения и исключения
        when(githubConfig.getIncludePatterns()).thenReturn(Arrays.asList("**/*.java"));
        when(githubConfig.getExcludePatterns()).thenReturn(Arrays.asList("target/**"));
    }

    @Test
    void shouldReturnRepositoryContentsWhenFilesMatchPatterns() {
        String repoUrl = "https://github.com/user/repo";

        // Мокаем вызов для получения содержимого репозитория
        when(restTemplate.exchange(
                eq("https://api.github.com/repos/user/repo/contents"),
                eq(HttpMethod.GET),
                eq(httpEntity),
                eq(List.class)
        )).thenReturn(ResponseEntity.ok(List.of(
                Map.of("type", "file", "path", "src/main/java/Test.java",
                        "download_url", "https://raw.githubusercontent.com/user/repo/main/src/main/java/Test.java")
        )));

        // Мокаем вызов для получения содержимого файла
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.set("Authorization", "Bearer test-token");
        HttpEntity<String> fileEntity = new HttpEntity<>(fileHeaders);
        when(restTemplate.exchange(
                eq("https://raw.githubusercontent.com/user/repo/main/src/main/java/Test.java"),
                eq(HttpMethod.GET),
                eq(fileEntity),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("public class Test {}"));

        // Вызываем метод сервиса
        List<String> result = githubContentService.getRepositoryContents(repoUrl);

        // Проверяем результат
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Result should not be empty"); // Добавлено сообщение для ясности
        assertTrue(result.get(0).contains("src/main/java/Test.java"));
        assertTrue(result.get(0).contains("public class Test {}"));
    }
}
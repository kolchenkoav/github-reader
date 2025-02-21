package com.example.githubreader.service;

import com.example.githubreader.config.GithubConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubContentServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GithubConfig githubConfig; // Added mock for GithubConfig

    @InjectMocks
    private GithubContentService githubContentService;

    private HttpEntity<String> httpEntity;

    @BeforeEach
    void setUp() {
        when(githubConfig.getToken()).thenReturn("test-token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer test-token");
        httpEntity = new HttpEntity<>(headers);
    }

    @Test
    void shouldReturnRepositoryContentsWhenFilesMatchPatterns() {
        String repoUrl = "https://github.com/user/repo";

        // Define the ParameterizedTypeReference
        ParameterizedTypeReference<List<Map<String, Object>>> typeRef =
                new ParameterizedTypeReference<>() {};

        // Set up HttpEntity with exact headers expected in the service
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer test-token");
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        // Mock the GitHub API response for repository contents
        when(restTemplate.exchange(
                eq("https://api.github.com/repos/user/repo/contents"),
                eq(HttpMethod.GET),
                eq(httpEntity),
                eq(typeRef)
        )).thenReturn(ResponseEntity.ok(List.of(
                Map.of("type", "file", "path", "src/main/java/Test.java",
                        "download_url", "https://raw.githubusercontent.com/user/repo/main/src/main/java/Test.java")
        )));

        // Mock the file content download
        when(restTemplate.exchange(
                eq("https://raw.githubusercontent.com/user/repo/main/src/main/java/Test.java"),
                eq(HttpMethod.GET),
                eq(httpEntity),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("public class Test {}"));

        // Mock nested directory contents (if applicable)
        when(restTemplate.exchange(
                eq("https://api.github.com/repos/user/repo/contents/src"),
                eq(HttpMethod.GET),
                eq(httpEntity),
                eq(typeRef)
        )).thenReturn(ResponseEntity.ok(List.of()));

        // Call the service method
        List<String> result = githubContentService.getRepositoryContents(repoUrl);

        // Add assertions as needed
        assertNotNull(result);
        // Add more assertions to verify the result
    }
}
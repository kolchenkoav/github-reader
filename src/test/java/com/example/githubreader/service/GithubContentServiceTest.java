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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubContentServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GithubConfig githubConfig;

    @InjectMocks
    private GithubContentService githubContentService;

    private HttpEntity<String> httpEntity;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        when(githubConfig.getToken()).thenReturn("test-token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer test-token");
        headers.set("Accept", "application/vnd.github.v3+json");
        httpEntity = new HttpEntity<>(headers);

        executorService = Executors.newVirtualThreadPerTaskExecutor();
        githubContentService = new GithubContentService(restTemplate, githubConfig);
    }

    @Test
    void shouldReturnRepositoryContentsWhenFilesMatchPatterns() throws Exception {
        when(githubConfig.getIncludePatterns()).thenReturn(Arrays.asList("**/*.java"));
        when(githubConfig.getExcludePatterns()).thenReturn(Arrays.asList("target/**"));

        String repoUrl = "https://github.com/user/repo";

        List<Map<String, Object>> mockFiles = Arrays.asList(
                Map.of("type", "file", "path", "src/main/java/Test.java",
                        "download_url", "https://raw.githubusercontent.com/user/repo/main/src/main/java/Test.java"),
                Map.of("type", "file", "path", "src/main/java/Another.java",
                        "download_url", "https://raw.githubusercontent.com/user/repo/main/src/main/java/Another.java")
        );
        when(restTemplate.exchange(
                eq("https://api.github.com/repos/user/repo/contents"),
                eq(HttpMethod.GET),
                eq(httpEntity),
                eq(List.class)
        )).thenReturn(ResponseEntity.ok(mockFiles));

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.set("Authorization", "Bearer test-token");
        HttpEntity<String> fileEntity = new HttpEntity<>(fileHeaders);

        when(restTemplate.exchange(
                eq("https://raw.githubusercontent.com/user/repo/main/src/main/java/Test.java"),
                eq(HttpMethod.GET),
                eq(fileEntity),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("public class Test {}"));

        when(restTemplate.exchange(
                eq("https://raw.githubusercontent.com/user/repo/main/src/main/java/Another.java"),
                eq(HttpMethod.GET),
                eq(fileEntity),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("public class Another {}"));

        CountDownLatch latch = new CountDownLatch(2);
        GithubContentService spyService = spy(githubContentService);
        doAnswer(invocation -> {
            String result = (String) invocation.callRealMethod();
            latch.countDown();
            return result;
        }).when(spyService).getFileContentFromUrl(anyString());

        List<String> result = spyService.getRepositoryContents(repoUrl);
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(2, result.size(), "Expected 2 files in the result");
        assertTrue(result.stream().anyMatch(content -> content.contains("src/main/java/Test.java") && content.contains("public class Test {}")));
        assertTrue(result.stream().anyMatch(content -> content.contains("src/main/java/Another.java") && content.contains("public class Another {}")));
    }

    @Test
    void shouldHandleDirectoryContentsInParallel() throws Exception {
        when(githubConfig.getIncludePatterns()).thenReturn(Arrays.asList("**/*.java"));
        when(githubConfig.getExcludePatterns()).thenReturn(Arrays.asList("target/**"));

        String repoUrl = "https://github.com/user/repo";

        List<Map<String, Object>> rootFiles = Arrays.asList(
                Map.of("type", "dir", "path", "src", "url", "https://api.github.com/repos/user/repo/contents/src")
        );
        when(restTemplate.exchange(
                eq("https://api.github.com/repos/user/repo/contents"),
                eq(HttpMethod.GET),
                eq(httpEntity),
                eq(List.class)
        )).thenReturn(ResponseEntity.ok(rootFiles));

        List<Map<String, Object>> dirFiles = Arrays.asList(
                Map.of("type", "file", "path", "src/main.java",
                        "download_url", "https://raw.githubusercontent.com/user/repo/main/src/main.java")
        );
        when(restTemplate.exchange(
                eq("https://api.github.com/repos/user/repo/contents/src"),
                eq(HttpMethod.GET),
                eq(httpEntity),
                eq(List.class)
        )).thenReturn(ResponseEntity.ok(dirFiles));

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.set("Authorization", "Bearer test-token");
        HttpEntity<String> fileEntity = new HttpEntity<>(fileHeaders);
        when(restTemplate.exchange(
                eq("https://raw.githubusercontent.com/user/repo/main/src/main.java"),
                eq(HttpMethod.GET),
                eq(fileEntity),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("public class Main {}"));

        CountDownLatch latch = new CountDownLatch(1);
        GithubContentService spyService = spy(githubContentService);
        doAnswer(invocation -> {
            String result = (String) invocation.callRealMethod();
            latch.countDown();
            return result;
        }).when(spyService).getFileContentFromUrl(anyString());

        List<String> result = spyService.getRepositoryContents(repoUrl);
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(1, result.size(), "Expected 1 file in the result");
        assertTrue(result.get(0).contains("src/main.java"));
        assertTrue(result.get(0).contains("public class Main {}"));
    }

    @Test
    void shouldThrowExceptionWhenApiCallFails() {
        String repoUrl = "https://github.com/user/repo";

        when(restTemplate.exchange(
                eq("https://api.github.com/repos/user/repo/contents"),
                eq(HttpMethod.GET),
                eq(httpEntity),
                eq(List.class)
        )).thenThrow(new RuntimeException("API failure"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            githubContentService.getRepositoryContents(repoUrl);
        });
        assertEquals("Failed to fetch repository contents", exception.getMessage());
        assertTrue(exception.getCause().getMessage().contains("API failure"));
    }
}
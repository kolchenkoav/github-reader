package com.example.githubreader.controller;

import com.example.githubreader.exception.GlobalExceptionHandler;
import com.example.githubreader.service.GithubContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GithubControllerTest {

    private MockMvc mockMvc;

    private GithubContentService githubContentService;

    @BeforeEach
    void setUp() {
        githubContentService = mock(GithubContentService.class);
        GithubController githubController = new GithubController(githubContentService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        mockMvc = MockMvcBuilders.standaloneSetup(githubController)
                .setControllerAdvice(exceptionHandler) // Добавляем обработчик исключений
                .build();
    }

    @Test
    @DisplayName("Should return file content for GET /content")
    void shouldReturnFileContent() throws Exception {
        String repoUrl = "https://github.com/user/repo";
        String filePath = "src/main/java/Test.java";
        String content = "public class Test {}";
        when(githubContentService.getFileContent(repoUrl, filePath)).thenReturn(content);

        mockMvc.perform(get("/api/github/content")
                        .param("repoUrl", repoUrl)
                        .param("filePath", filePath))
                .andExpect(status().isOk())
                .andExpect(content().string(content));
    }

    @Test
    @DisplayName("Should return repository contents for GET /repo-contents")
    void shouldReturnRepoContents() throws Exception {
        String repoUrl = "https://github.com/user/repo";
        List<String> contents = List.of(
                "File: <a href=\"https://github.com/user/repo/blob/main/src/main/java/Test.java\">src/main/java/Test.java</a> \npublic class Test {}\n"
        );
        when(githubContentService.getRepositoryContents(repoUrl)).thenReturn(contents);

        mockMvc.perform(get("/api/github/repo-contents")
                        .param("repoUrl", repoUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(containsString("src/main/java/Test.java")));
    }

    @Test
    @DisplayName("Should save contents to files for POST /save-contents")
    void shouldSaveRepoContents() throws Exception {
        String repoUrl = "https://github.com/user/repo";
        doNothing().when(githubContentService).saveRepositoryContents(repoUrl);

        mockMvc.perform(post("/api/github/save-contents")
                        .param("repoUrl", repoUrl))
                .andExpect(status().isOk())
                .andExpect(content().string("Contents saved successfully to output directory"));
    }

    @Test
    @DisplayName("Should save all contents to single file for POST /save-all-to-single-file")
    void shouldSaveAllToSingleFile() throws Exception {
        String repoUrl = "https://github.com/user/repo";
        doNothing().when(githubContentService).saveAllContentsToSingleFile(repoUrl);

        mockMvc.perform(post("/api/github/save-all-to-single-file")
                        .param("repoUrl", repoUrl))
                .andExpect(status().isOk())
                .andExpect(content().string("All contents saved successfully to single file"));
    }

    @Test
    @DisplayName("Should return 500 when saveAllToSingleFile throws exception")
    void shouldReturnErrorWhenSaveAllToSingleFileFails() throws Exception {
        String repoUrl = "https://github.com/user/repo";
        doThrow(new RuntimeException("Simulated failure")).when(githubContentService).saveAllContentsToSingleFile(repoUrl);

        mockMvc.perform(post("/api/github/save-all-to-single-file")
                        .param("repoUrl", repoUrl))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Internal Server Error: Simulated failure")));
    }
}
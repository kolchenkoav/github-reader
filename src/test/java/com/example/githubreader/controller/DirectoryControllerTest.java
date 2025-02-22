package com.example.githubreader.controller;

import com.example.githubreader.exception.GlobalExceptionHandler;
import com.example.githubreader.service.DirectoryContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DirectoryControllerTest {

    private MockMvc mockMvc;

    private DirectoryContentService directoryContentService;

    @BeforeEach
    void setUp() {
        directoryContentService = mock(DirectoryContentService.class);
        DirectoryController directoryController = new DirectoryController(directoryContentService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        mockMvc = MockMvcBuilders.standaloneSetup(directoryController)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    @DisplayName("Should save directory contents using default path")
    void shouldSaveDirectoryContentsWithDefaultPath() throws Exception {
        doNothing().when(directoryContentService).saveAllContentsToFile(null);

        mockMvc.perform(post("/api/directory/save-all-to-file"))
                .andExpect(status().isOk())
                .andExpect(content().string("All directory contents saved successfully to file"));

        verify(directoryContentService, times(1)).saveAllContentsToFile(null);
    }

    @Test
    @DisplayName("Should save directory contents with provided encoded path")
    void shouldSaveDirectoryContentsWithProvidedPath() throws Exception {
        String encodedPath = "E:%5Cprojects%5Cjava%5Cp3_tariff_calculator";
        String decodedPath = "E:\\projects\\java\\p3_tariff_calculator";
        doNothing().when(directoryContentService).saveAllContentsToFile(decodedPath);

        mockMvc.perform(post("/api/directory/save-all-to-file")
                        .param("directoryPath", encodedPath))
                .andExpect(status().isOk())
                .andExpect(content().string("All directory contents saved successfully to file"));

        verify(directoryContentService, times(1)).saveAllContentsToFile(decodedPath);
    }

    @Test
    @DisplayName("Should return 500 when service throws exception")
    void shouldReturnErrorWhenServiceFails() throws Exception {
        doThrow(new RuntimeException("Simulated failure")).when(directoryContentService).saveAllContentsToFile(null);

        mockMvc.perform(post("/api/directory/save-all-to-file"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Internal Server Error: Simulated failure")));
    }
}
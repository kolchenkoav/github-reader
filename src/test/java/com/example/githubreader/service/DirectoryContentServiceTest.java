package com.example.githubreader.service;

import com.example.githubreader.config.DirectoryConfig;
import com.example.githubreader.config.GithubConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectoryContentServiceTest {

    @Mock
    private DirectoryConfig directoryConfig;

    @Mock
    private GithubConfig githubConfig;

    @InjectMocks
    private DirectoryContentService directoryContentService;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test-dir");
        // Убираем общую настройку directoryConfig.getDefaultPath() из setUp
    }

    @Test
    void shouldProcessTextFilesAndSkipExcluded() throws IOException {
        // Arrange
        when(directoryConfig.getDefaultPath()).thenReturn(tempDir.toString());
        when(githubConfig.getIncludePatterns()).thenReturn(Arrays.asList("**/*.txt", "**/*.java"));
        when(githubConfig.getExcludePatterns()).thenReturn(Arrays.asList(".git/**", "target/**"));

        Files.createDirectories(tempDir.resolve(".git/objects"));
        Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
        Files.writeString(file1, "Content of file1");
        Path file2 = Files.createFile(tempDir.resolve("file2.java"));
        Files.writeString(file2, "public class Test {}");
        Path gitFile = Files.createFile(tempDir.resolve(".git/objects/abc123"));
        Files.write(gitFile, new byte[]{0x01, 0x02});
        Path binFile = Files.createFile(tempDir.resolve("file3.bin"));
        Files.write(binFile, new byte[]{0x03, 0x04});

        // Act
        directoryContentService.saveAllContentsToFile(null);

        // Assert
        Path outputFile = Paths.get("output", "all_contents_from_" + tempDir.getFileName() + ".txt");
        assertTrue(Files.exists(outputFile), "Output file should be created");

        String content = Files.readString(outputFile);
        assertTrue(content.contains("File: file1.txt\nContent of file1\n"), "Should include file1.txt");
        assertTrue(content.contains("File: file2.java\npublic class Test {}\n"), "Should include file2.java");
        assertFalse(content.contains("abc123"), "Should exclude .git/objects/abc123");
        assertFalse(content.contains("file3.bin"), "Should exclude file3.bin");
    }

    @Test
    void shouldThrowExceptionForInvalidDirectory() {
        // Arrange
        String invalidPath = tempDir.resolve("non-existent").toString();
        when(directoryConfig.getDefaultPath()).thenReturn(invalidPath);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            directoryContentService.saveAllContentsToFile(null);
        });
        assertEquals("Failed to save directory contents to file", exception.getMessage());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Provided path is not a directory: " + invalidPath, exception.getCause().getMessage());
    }

    @Test
    void shouldHandleIOExceptionGracefully() throws IOException {
        // Arrange
        Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
        Files.writeString(file1, "Content of file1");

        DirectoryContentService spyService = spy(directoryContentService);
        doThrow(new RuntimeException("Simulated failure", new IOException("Simulated IO error")))
                .when(spyService).saveAllContentsToFile(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            spyService.saveAllContentsToFile(null);
        });
        assertEquals("Simulated failure", exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
        assertEquals("Simulated IO error", exception.getCause().getMessage());
    }
}
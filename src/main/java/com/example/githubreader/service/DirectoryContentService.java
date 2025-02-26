// File: src/main/java/com/example/githubreader/service/DirectoryContentService.java
package com.example.githubreader.service;

import com.example.githubreader.config.GithubConfig;
import com.example.githubreader.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Сервис для работы с содержимым локальных директорий.
 */
@Service
public class DirectoryContentService {
    private static final Logger log = LoggerFactory.getLogger(DirectoryContentService.class);

    @Value("${file.output.path}")
    private String outputPath;

    @Value("${file.name}")
    private String fileName;

    private final GithubConfig githubConfig;  // Добавляем GithubConfig для паттернов
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public DirectoryContentService(GithubConfig githubConfig) {
        this.githubConfig = githubConfig;
    }

    /**
     * Считывает содержимое всех файлов из директории и сохраняет в один файл.
     *
     * @param directoryPath Путь к директории (опционально, если не указан — используется defaultPath)
     */
    public void saveAllContentsToFile(String directoryPath) {
        if (directoryPath == null) {
            throw new IllegalArgumentException("Directory path is not provided and defaultPath is not configured");
        }

        try {
            Path dirPath = Paths.get(directoryPath);
            if (!Files.isDirectory(dirPath)) {
                throw new IllegalArgumentException("Provided path is not a directory: " + directoryPath);
            }

            List<String> contents = new ArrayList<>();
            List<Future<?>> futures = new ArrayList<>();

            Files.walk(dirPath)
                    .filter(Files::isRegularFile)
                    .filter(this::matchesPatterns)  // Добавляем фильтр по паттернам
                    .forEach(filePath -> {
                        Future<?> future = executorService.submit(() -> {
                            try {
                                String content = Files.readString(filePath);
                                String relativePath = dirPath.relativize(filePath).toString();
                                String formattedContent = "File: " + relativePath + "\n" + content + "\n";
                                synchronized (contents) {
                                    contents.add(formattedContent);
                                }
                                log.debug("Processed file: {}", relativePath);
                            } catch (IOException e) {
                                log.error("Failed to read file {}: {}", filePath, e.getMessage());
                            }
                        });
                        futures.add(future);
                    });

            for (Future<?> future : futures) {
                future.get();
            }

            // Нормализуем путь в зависимости от ОС
            Path directory = Paths.get(outputPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.info("Created directory: {}", directory);
            }

            Path filePath = directory.resolve(fileName + "directory_" + PathUtils.getNameDir(directoryPath) + ".txt");
            String allContent = contents.stream().collect(Collectors.joining("\n"));
            Files.writeString(filePath, allContent);

            log.info("Saved all directory contents to file: {}", outputPath);

        } catch (Exception e) {
            log.error("Failed to process directory {}: {}", directoryPath, e.getMessage());
            throw new RuntimeException("Failed to save directory contents to file", e);
        }
    }

    /**
     * Проверяет, соответствует ли путь include и exclude паттернам.
     *
     * @param filePath Путь к файлу
     * @return true, если файл должен быть включен
     */
    private boolean matchesPatterns(Path filePath) {
        String path = filePath.toString().replace("\\", "/"); // Нормализуем слеши для совместимости
        for (String exclude : githubConfig.getExcludePatterns()) {
            if (matchesPattern(path, exclude)) {
                return false;
            }
        }
        for (String include : githubConfig.getIncludePatterns()) {
            if (matchesPattern(path, include)) {
                return true;
            }
        }
        return false; // Если не соответствует включающим паттернам, исключаем
    }

    /**
     * Проверяет, соответствует ли путь заданному паттерну.
     *
     * @param path Путь к файлу
     * @param pattern Паттерн для проверки
     * @return true, если путь соответствует паттерну
     */
    private boolean matchesPattern(String path, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
                .replace("/", "[/\\\\]"); // Учитываем оба типа слешей
        return path.matches(".*" + regex + ".*"); // Добавляем .* для проверки частичного совпадения
    }
}
package com.example.githubreader.service;

import com.example.githubreader.config.GithubConfig;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Сервис для работы с содержимым репозиториев GitHub.
 */
@Slf4j
@Service
public class GithubContentService {

    private final RestTemplate restTemplate;
    private final GithubConfig githubConfig;
    private final ExecutorService executorService;

    public GithubContentService(RestTemplate restTemplate, GithubConfig githubConfig) {
        this.restTemplate = restTemplate;
        this.githubConfig = githubConfig;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    private static final String GITHUB_API_URL = "https://api.github.com/repos/";
    private static final String OUTPUT_DIR = "output/";

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Получает содержимое файла из репозитория GitHub.
     *
     * @param repoUrl URL репозитория.
     * @param filePath Путь к файлу в репозитории.
     * @return Содержимое файла.
     */
    public String getFileContent(String repoUrl, String filePath) {
        String apiUrl = convertToApiUrl(repoUrl) + "/contents/" + filePath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());
        headers.set("Accept", "application/vnd.github.v3.raw");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                entity,
                String.class
        ).getBody();
    }

    /**
     * Получает список содержимого репозитория.
     *
     * @param repoUrl URL репозитория.
     * @return Список содержимого репозитория.
     */
    public List<String> getRepositoryContents(String repoUrl) {
        List<String> contents = new ArrayList<>();
        String apiUrl = convertToApiUrl(repoUrl) + "/contents";
        String htmlBaseUrl = convertToHtmlBaseUrl(repoUrl);

        fetchAndProcessContents(apiUrl, contents, htmlBaseUrl);
        return contents;
    }

    /**
     * Сохраняет содержимое репозитория в файлы.
     *
     * @param repoUrl URL репозитория.
     */
    public void saveRepositoryContents(String repoUrl) {
        String apiUrl = convertToApiUrl(repoUrl) + "/contents";
        fetchAndSaveContents(apiUrl);
    }

    /**
     * Сохраняет все содержимое репозитория в один файл.
     *
     * @param repoUrl URL репозитория.
     */
    public void saveAllContentsToSingleFile(String repoUrl) {
        List<String> contents = new ArrayList<>();
        String apiUrl = convertToApiUrl(repoUrl) + "/contents";

        fetchAndCollectContents(apiUrl, contents);

        try {
            Path filePath = Paths.get(githubConfig.getSingleFilePath());
            Files.createDirectories(filePath.getParent()); // Создаем родительские директории
            String allContent = contents.stream().collect(Collectors.joining("\n"));
            Files.writeString(filePath, allContent);
            log.info("Saved all contents to single file: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save all contents to {}: {}", githubConfig.getSingleFilePath(), e.getMessage());
            throw new RuntimeException("Failed to save all contents to single file", e);
        }
    }

    /**
     * Получает и обрабатывает содержимое репозитория.
     *
     * @param apiUrl URL API для получения содержимого репозитория.
     * @param contents Список для хранения содержимого файлов.
     * @param htmlBaseUrl Базовый URL для HTML-ссылок на файлы.
     */
    private void fetchAndProcessContents(String apiUrl, List<String> contents, String htmlBaseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            List<Map<String, Object>> files = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, entity, List.class
            ).getBody();

            if (files != null) {
                List<Future<?>> futures = new ArrayList<>();
                for (Map<String, Object> file : files) {
                    Future<?> future = executorService.submit(() -> {
                        String type = (String) file.get("type");
                        String path = (String) file.get("path");
                        if ("file".equals(type) && matchesPatterns(path)) {
                            String content = getFileContentFromUrl((String) file.get("download_url"));
                            if (content != null) {
                                String fileLink = htmlBaseUrl + "/blob/main/" + path;
                                String formattedLine = "File: <a href=\"" + fileLink + "\">" + path + "</a> \n" + content + "\n";
                                synchronized (contents) { // Синхронизация для потокобезопасности
                                    contents.add(formattedLine);
                                }
                                log.debug("Обработан файл: {}", path);
                            }
                        } else if ("dir".equals(type)) {
                            processDirectory((String) file.get("url"), contents, htmlBaseUrl, false);
                        }
                    });
                    futures.add(future);
                }
                // Ожидаем завершения всех задач
                for (Future<?> future : futures) {
                    future.get();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching repository contents from {}: {}", apiUrl, e.getMessage());
            throw new RuntimeException("Failed to fetch repository contents", e);
        }
    }

    /**
     * Получает и сохраняет содержимое репозитория.
     *
     * @param apiUrl URL API для получения содержимого репозитория.
     */
    private void fetchAndSaveContents(String apiUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            List<Map<String, Object>> files = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, entity, List.class
            ).getBody();

            if (files != null) {
                List<Future<?>> futures = new ArrayList<>();
                for (Map<String, Object> file : files) {
                    Future<?> future = executorService.submit(() -> {
                        String type = (String) file.get("type");
                        String path = (String) file.get("path");
                        if ("file".equals(type) && matchesPatterns(path)) {
                            String content = getFileContentFromUrl((String) file.get("download_url"));
                            if (content != null) {
                                saveContentToFile(path, content);
                            }
                        } else if ("dir".equals(type)) {
                            processDirectory((String) file.get("url"), new ArrayList<>(), "", true);
                        }
                    });
                    futures.add(future);
                }
                for (Future<?> future : futures) {
                    future.get();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching repository contents for saving from {}: {}", apiUrl, e.getMessage());
            throw new RuntimeException("Failed to save repository contents", e);
        }
    }

    /**
     * Получает и собирает содержимое репозитория для сохранения в один файл.
     *
     * @param apiUrl URL API для получения содержимого репозитория.
     * @param contents Список для хранения содержимого файлов.
     */
    private void fetchAndCollectContents(String apiUrl, List<String> contents) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            List<Map<String, Object>> files = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, entity, List.class
            ).getBody();

            if (files != null) {
                List<Future<?>> futures = new ArrayList<>();
                for (Map<String, Object> file : files) {
                    Future<?> future = executorService.submit(() -> {
                        String type = (String) file.get("type");
                        String path = (String) file.get("path");
                        if ("file".equals(type) && matchesPatterns(path)) {
                            String content = getFileContentFromUrl((String) file.get("download_url"));
                            if (content != null) {
                                synchronized (contents) {
                                    contents.add("File: " + path + " \n" + content + "\n");
                                }
                                log.debug("Collected file for single file: {}", path);
                            }
                        } else if ("dir".equals(type)) {
                            processDirectoryForSingleFile((String) file.get("url"), contents);
                        }
                    });
                    futures.add(future);
                }
                for (Future<?> future : futures) {
                    future.get();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching repository contents for single file from {}: {}", apiUrl, e.getMessage());
            throw new RuntimeException("Failed to fetch repository contents for single file", e);
        }
    }

    /**
     * Обрабатывает файлы и директории в репозитории.
     *
     * @param files Список файлов и директорий.
     * @param contents Список для хранения содержимого файлов.
     * @param htmlBaseUrl Базовый URL для HTML-ссылок на файлы.
     * @param saveToFile Флаг, указывающий, нужно ли сохранять содержимое файлов.
     */
    private void processFiles(List<Map<String, Object>> files, List<String> contents, String htmlBaseUrl, boolean saveToFile) {
        for (Map<String, Object> file : files) {
            String type = (String) file.get("type");
            String path = (String) file.get("path");

            if ("file".equals(type)) {
                if (matchesPatterns(path)) {
                    String content = getFileContentFromUrl((String) file.get("download_url"));
                    if (content != null) {
                        if (saveToFile) {
                            saveContentToFile(path, content);
                        } else {
                            String fileLink = htmlBaseUrl + "/blob/main/" + path;
                            String formattedLine = "File: <a href=\"" + fileLink + "\">" + path + "</a> \n" + content + "\n";
                            contents.add(formattedLine);
                        }
                        log.debug("Обработан файл: {}", path);
                    }
                } else {
                    log.debug("Исключен файл по шаблону: {}", path);
                }
            } else if ("dir".equals(type)) {
                processDirectory((String) file.get("url"), contents, htmlBaseUrl, saveToFile);
            }
        }
    }

    /**
     * Обрабатывает файлы и директории для сохранения в один файл.
     *
     * @param files Список файлов и директорий.
     * @param contents Список для хранения содержимого файлов.
     */
    private void processFilesForSingleFile(List<Map<String, Object>> files, List<String> contents) {
        for (Map<String, Object> file : files) {
            String type = (String) file.get("type");
            String path = (String) file.get("path");

            if ("file".equals(type)) {
                if (matchesPatterns(path)) {
                    String content = getFileContentFromUrl((String) file.get("download_url"));
                    if (content != null) {
                        contents.add("File: " + path + " \n" + content + "\n");
                        log.debug("Collected file for single file: {}", path);
                    }
                } else {
                    log.debug("Исключен файл по шаблону: {}", path);
                }
            } else if ("dir".equals(type)) {
                processDirectoryForSingleFile((String) file.get("url"), contents);
            }
        }
    }

    /**
     * Обрабатывает директорию в репозитории.
     *
     * @param dirUrl URL директории.
     * @param contents Список для хранения содержимого файлов.
     * @param htmlBaseUrl Базовый URL для HTML-ссылок на файлы.
     * @param saveToFile Флаг, указывающий, нужно ли сохранять содержимое файлов.
     */
    private void processDirectory(String dirUrl, List<String> contents, String htmlBaseUrl, boolean saveToFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            List<Map<String, Object>> files = restTemplate.exchange(
                    dirUrl,
                    HttpMethod.GET,
                    entity,
                    List.class
            ).getBody();

            if (files != null) {
                processFiles(files, contents, htmlBaseUrl, saveToFile);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке директории {}: {}", dirUrl, e.getMessage());
        }
    }

    /**
     * Обрабатывает директорию в репозитории для сохранения в один файл.
     *
     * @param dirUrl URL директории.
     * @param contents Список для хранения содержимого файлов.
     */
    private void processDirectoryForSingleFile(String dirUrl, List<String> contents) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            List<Map<String, Object>> files = restTemplate.exchange(
                    dirUrl,
                    HttpMethod.GET,
                    entity,
                    List.class
            ).getBody();

            if (files != null) {
                processFilesForSingleFile(files, contents);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке директории для одного файла {}: {}", dirUrl, e.getMessage());
        }
    }

    /**
     * Сохраняет содержимое файла в указанный путь.
     *
     * @param path Путь, по которому будет сохранен файл.
     * @param content Содержимое файла.
     */
    private void saveContentToFile(String path, String content) {
        try {
            Path filePath = Paths.get(OUTPUT_DIR, path);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
            log.info("Сохранен файл: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save file {}: {}", path, e.getMessage());
            throw new RuntimeException("Failed to save file: " + path, e);
        }
    }

    /**
     * Получает содержимое файла по указанному URL.
     *
     * @param downloadUrl URL для загрузки содержимого файла.
     * @return Содержимое файла или null, если произошла ошибка.
     */
    String getFileContentFromUrl(String downloadUrl) {
        if (downloadUrl == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            return restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            ).getBody();
        } catch (Exception e) {
            log.error("Ошибка при получении содержимого файла из {}: {}", downloadUrl, e.getMessage());
            return null;
        }
    }

    private boolean matchesPatterns(String path) {
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
        return false;
    }

    private boolean matchesPattern(String path, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        return path.matches(regex);
    }

    private String convertToApiUrl(String repoUrl) {
        if (repoUrl.endsWith(".git")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
        }
        return repoUrl.replace("https://github.com/", GITHUB_API_URL);
    }

    private String convertToHtmlBaseUrl(String repoUrl) {
        if (repoUrl.endsWith(".git")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
        }
        return repoUrl;
    }
}
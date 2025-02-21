package com.example.githubreader.service;

import com.example.githubreader.config.GithubConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;

/**
 * Сервис для работы с содержимым репозиториев GitHub.
 */
@Service
@RequiredArgsConstructor
public class GithubContentService {

    private static final Logger logger = LoggerFactory.getLogger(GithubContentService.class);
    private final RestTemplate restTemplate;
    private final GithubConfig githubConfig;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/";
    private static final String GITHUB_HTML_BASE_URL = "https://github.com/";
    private static final String OUTPUT_DIR = "output/";

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
            logger.info("Saved all contents to single file: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to save all contents to {}: {}", githubConfig.getSingleFilePath(), e.getMessage());
            throw new RuntimeException("Failed to save all contents to single file", e);
        }
    }

    private void fetchAndProcessContents(String apiUrl, List<String> contents, String htmlBaseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            List<Map<String, Object>> files = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    List.class
            ).getBody();

            if (files != null) {
                processFiles(files, contents, htmlBaseUrl, false);
            }
        } catch (Exception e) {
            logger.error("Error fetching repository contents from {}: {}", apiUrl, e.getMessage());
            throw new RuntimeException("Failed to fetch repository contents", e);
        }
    }

    private void fetchAndSaveContents(String apiUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            List<Map<String, Object>> files = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    List.class
            ).getBody();

            if (files != null) {
                processFiles(files, new ArrayList<>(), "", true);
            }
        } catch (Exception e) {
            logger.error("Error fetching repository contents for saving from {}: {}", apiUrl, e.getMessage());
            throw new RuntimeException("Failed to save repository contents", e);
        }
    }

    private void fetchAndCollectContents(String apiUrl, List<String> contents) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubConfig.getToken());
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            List<Map<String, Object>> files = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    List.class
            ).getBody();

            if (files != null) {
                processFilesForSingleFile(files, contents);
            }
        } catch (Exception e) {
            logger.error("Error fetching repository contents for single file from {}: {}", apiUrl, e.getMessage());
            throw new RuntimeException("Failed to fetch repository contents for single file", e);
        }
    }

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
                        logger.debug("Обработан файл: {}", path);
                    }
                } else {
                    logger.debug("Исключен файл по шаблону: {}", path);
                }
            } else if ("dir".equals(type)) {
                processDirectory((String) file.get("url"), contents, htmlBaseUrl, saveToFile);
            }
        }
    }

    private void processFilesForSingleFile(List<Map<String, Object>> files, List<String> contents) {
        for (Map<String, Object> file : files) {
            String type = (String) file.get("type");
            String path = (String) file.get("path");

            if ("file".equals(type)) {
                if (matchesPatterns(path)) {
                    String content = getFileContentFromUrl((String) file.get("download_url"));
                    if (content != null) {
                        contents.add("File: " + path + " \n" + content + "\n");
                        logger.debug("Collected file for single file: {}", path);
                    }
                } else {
                    logger.debug("Исключен файл по шаблону: {}", path);
                }
            } else if ("dir".equals(type)) {
                processDirectoryForSingleFile((String) file.get("url"), contents);
            }
        }
    }

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
            logger.error("Ошибка при обработке директории {}: {}", dirUrl, e.getMessage());
        }
    }

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
            logger.error("Ошибка при обработке директории для одного файла {}: {}", dirUrl, e.getMessage());
        }
    }

    private void saveContentToFile(String path, String content) {
        try {
            Path filePath = Paths.get(OUTPUT_DIR, path);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
            logger.info("Сохранен файл: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to save file {}: {}", path, e.getMessage());
            throw new RuntimeException("Failed to save file: " + path, e);
        }
    }

    private String getFileContentFromUrl(String downloadUrl) {
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
            logger.error("Ошибка при получении содержимого файла из {}: {}", downloadUrl, e.getMessage());
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
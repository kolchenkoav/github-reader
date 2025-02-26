package com.example.githubreader.controller;

import com.example.githubreader.service.GithubContentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для работы с GitHub репозиториями.
 */
@RestController
@RequestMapping("/api/github")
public class GithubController {

    private final GithubContentService githubContentService;

    public GithubController(GithubContentService githubContentService) {
        this.githubContentService = githubContentService;
    }

    /**
     * Получает содержимое файла из GitHub репозитория.
     *
     * @param repoUrl URL репозитория.
     * @param filePath Путь к файлу в репозитории.
     * @return Содержимое файла.
     */
    @GetMapping("/content")
    public String getFileContent(
            @RequestParam String repoUrl,
            @RequestParam String filePath
    ) {
        return githubContentService.getFileContent(repoUrl, filePath);
    }

    /**
     * Получает список содержимого репозитория.
     *
     * @param repoUrl URL репозитория.
     * @return Список содержимого репозитория.
     */
    @GetMapping("/repo-contents")
    public List<String> getRepoContents(
            @RequestParam String repoUrl
    ) {
        return githubContentService.getRepositoryContents(repoUrl);
    }

    /**
     * Сохраняет содержимое репозитория в выходную директорию.
     *
     * @param repoUrl URL репозитория.
     * @return Сообщение об успешном сохранении.
     */
    @PostMapping("/save-contents")
    public String saveRepoContents(
            @RequestParam String repoUrl
    ) {
        githubContentService.saveRepositoryContents(repoUrl);
        return "Contents saved successfully to output directory";
    }

    /**
     * Сохраняет все содержимое репозитория в один файл.
     *
     * @param repoUrl URL репозитория.
     * @return Сообщение об успешном сохранении.
     */
    @PostMapping("/save-all-to-single-file")
    public String saveAllToSingleFile(
            @RequestParam String repoUrl
    ) {
        githubContentService.saveAllContentsToSingleFile(repoUrl);
        return "All contents saved successfully to single file";
    }
}
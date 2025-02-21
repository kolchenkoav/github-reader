package com.example.githubreader.controller;

import com.example.githubreader.service.GithubContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubController {

    private final GithubContentService githubContentService;

    @GetMapping("/content")
    public String getFileContent(
            @RequestParam String repoUrl,
            @RequestParam String filePath
    ) {
        return githubContentService.getFileContent(repoUrl, filePath);
    }

    @GetMapping("/repo-contents")
    public List<String> getRepoContents(
            @RequestParam String repoUrl
    ) {
        return githubContentService.getRepositoryContents(repoUrl);
    }

    @PostMapping("/save-contents")
    public String saveRepoContents(
            @RequestParam String repoUrl
    ) {
        githubContentService.saveRepositoryContents(repoUrl);
        return "Contents saved successfully to output directory";
    }

    @PostMapping("/save-all-to-single-file")
    public String saveAllToSingleFile(
            @RequestParam String repoUrl
    ) {
        githubContentService.saveAllContentsToSingleFile(repoUrl);
        return "All contents saved successfully to single file";
    }
}
package com.example.githubreader.model;

import lombok.Data;

@Data
public class ContentSourceRequest {
    private String sourceType; // "github" или "directory"
    private String path;       // repoUrl или directoryPath в зависимости от sourceType
}

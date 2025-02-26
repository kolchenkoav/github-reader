package com.example.githubreader.model;

public class ContentSourceRequest {
    private String sourceType; // "github" или "directory"
    private String path;       // repoUrl или directoryPath в зависимости от sourceType

    public ContentSourceRequest() {
    }

    public ContentSourceRequest(String sourceType, String path) {
        this.sourceType = sourceType;
        this.path = path;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

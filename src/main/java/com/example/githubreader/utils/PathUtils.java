package com.example.githubreader.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    public static String getNameDir(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }

        // Удаляем завершающий слэш, если он есть
        String cleanedPath = path.trim().replaceAll("[/\\\\]+$", "");

        // Преобразуем строку в объект Path
        Path filePath = Paths.get(cleanedPath);

        // Получаем имя последней части пути
        Path fileName = filePath.getFileName();
        if (fileName == null) {
            return ""; // Если путь пустой или корневой (например, "C:\\")
        }

        return fileName.toString();
    }

    public static String getNameRepo(String repo) {
        if (repo == null || repo.trim().isEmpty()) {
            return "";
        }

        // Удаляем завершающий слэш и суффикс '.git', если они есть
        String cleanedRepo = repo.trim()
                .replaceAll("/+$", "")  // Удаляем завершающие слэши
                .replaceAll("\\.git$", "");  // Удаляем .git

        // Разделяем строку по слэшам
        String[] parts = cleanedRepo.split("[/\\\\]");
        if (parts.length == 0) {
            return "";
        }

        // Возвращаем последнюю часть
        return parts[parts.length - 1];
    }

}

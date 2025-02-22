package com.example.githubreader.controller;

import com.example.githubreader.service.DirectoryContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Контроллер для работы с содержимым локальных директорий.
 */
@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryContentService directoryContentService;

    /**
     * Считывает содержимое всех файлов из указанной директории и сохраняет в один файл.
     *
     * @param directoryPath Путь к директории (опционально, по умолчанию из конфигурации)
     * @return Сообщение об успешном сохранении
     */
    @PostMapping("/save-all-to-file")
    public String saveDirectoryContentsToFile(
            @RequestParam(required = false) String directoryPath
    ) {
        String decodedPath = directoryPath != null
                ? URLDecoder.decode(directoryPath, StandardCharsets.UTF_8)
                : null;
        directoryContentService.saveAllContentsToFile(decodedPath);
        return "All directory contents saved successfully to file";
    }
}
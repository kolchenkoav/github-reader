package com.example.githubreader.controller;

import com.example.githubreader.model.ContentSourceRequest;
import com.example.githubreader.service.DirectoryContentService;
import com.example.githubreader.service.GithubContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Контроллер для работы с формой выбора источника контента.
 */
@Controller
public class ContentFormController {

    private final GithubContentService githubContentService;
    private final DirectoryContentService directoryContentService;

    @Autowired
    public ContentFormController(GithubContentService githubContentService, DirectoryContentService directoryContentService) {
        this.githubContentService = githubContentService;
        this.directoryContentService = directoryContentService;
    }

    /**
     * Переадресация с корневого пути на /content-form.
     */
    @GetMapping("/")
    public String redirectToContentForm() {
        return "redirect:/content-form";
    }

    /**
     * Отображает форму для выбора источника контента.
     */
    @GetMapping("/content-form")
    public String showForm(Model model) {
        if (!model.containsAttribute("contentRequest")) {
            model.addAttribute("contentRequest", new ContentSourceRequest());
        }
        model.addAttribute("message", null); // Очищаем сообщение при загрузке
        return "content-form";
    }

    /**
     * Обрабатывает отправку формы и вызывает соответствующий сервис.
     */
    @PostMapping("/content-form")
    public String submitForm(@ModelAttribute ContentSourceRequest contentRequest, Model model) {
        try {
            if ("github".equalsIgnoreCase(contentRequest.getSourceType())) {
                githubContentService.saveAllContentsToSingleFile(contentRequest.getPath());
            } else if ("directory".equalsIgnoreCase(contentRequest.getSourceType())) {
                directoryContentService.saveAllContentsToFile(contentRequest.getPath());
            } else {
                throw new IllegalArgumentException("Invalid source type: " + contentRequest.getSourceType());
            }
            model.addAttribute("message", "Content saved successfully to single file");
            model.addAttribute("isSuccess", true);
        } catch (Exception e) {
            model.addAttribute("message", "Ошибка при сохранении: " + e.getMessage());
            model.addAttribute("isSuccess", false);
        }
        model.addAttribute("contentRequest", contentRequest);
        return "content-form";
    }
}
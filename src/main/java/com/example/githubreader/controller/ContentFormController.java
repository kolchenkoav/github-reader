package com.example.githubreader.controller;

import com.example.githubreader.model.ContentSourceRequest;
import com.example.githubreader.service.DirectoryContentService;
import com.example.githubreader.service.GithubContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ContentFormController {

    private final GithubContentService githubContentService;
    private final DirectoryContentService directoryContentService;
    private final Environment environment;

    @Value("${file.output.path}")
    private String outputPath; // Получаем путь из application.yml

    @Autowired
    public ContentFormController(GithubContentService githubContentService,
                                 DirectoryContentService directoryContentService,
                                 Environment environment) {
        this.githubContentService = githubContentService;
        this.directoryContentService = directoryContentService;
        this.environment = environment;
    }

    @GetMapping("/")
    public String redirectToContentForm() {
        return "redirect:/content-form";
    }

    @GetMapping("/content-form")
    public String showForm(Model model) {
        if (!model.containsAttribute("contentRequest")) {
            model.addAttribute("contentRequest", new ContentSourceRequest());
        }
        model.addAttribute("message", null);
        model.addAttribute("isDockerProfile", "docker".equals(environment.getActiveProfiles()[0]));
        // outputPath = "c:/tmp/output/"; Только для профиля docker
        model.addAttribute("outputPath", outputPath); // Передаем путь в модель
        return "content-form";
    }

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
        model.addAttribute("isDockerProfile", "docker".equals(environment.getActiveProfiles()[0]));
        model.addAttribute("outputPath", outputPath); // Передаем путь в модель
        return "content-form";
    }
}
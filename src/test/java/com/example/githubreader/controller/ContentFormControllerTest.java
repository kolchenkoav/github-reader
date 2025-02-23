package com.example.githubreader.controller;

import com.example.githubreader.service.DirectoryContentService;
import com.example.githubreader.service.GithubContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ContentFormControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GithubContentService githubContentService;

    @Mock
    private DirectoryContentService directoryContentService;

    @InjectMocks
    private ContentFormController contentFormController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contentFormController)
                .setViewResolvers(thymeleafViewResolver())
                .build();
    }

    private ThymeleafViewResolver thymeleafViewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setOrder(1);
        return viewResolver;
    }

    private SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        return templateEngine;
    }

    private ClassLoaderTemplateResolver templateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    @Test
    @DisplayName("Should redirect from root to /content-form")
    void shouldRedirectFromRootToContentForm() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/content-form"));
    }

    @Test
    @DisplayName("Should display content form on GET /content-form")
    void shouldDisplayContentForm() throws Exception {
        mockMvc.perform(get("/content-form"))
                .andExpect(status().isOk())
                .andExpect(view().name("content-form"))
                .andExpect(model().attributeExists("contentRequest"))
                .andExpect(model().attribute("message", nullValue()));
    }

    @Test
    @DisplayName("Should process form submission successfully for GitHub source")
    void shouldProcessGithubFormSubmissionSuccessfully() throws Exception {
        doNothing().when(githubContentService).saveAllContentsToSingleFile("https://github.com/user/repo");

        mockMvc.perform(post("/content-form")
                        .param("sourceType", "github")
                        .param("path", "https://github.com/user/repo"))
                .andExpect(status().isOk())
                .andExpect(view().name("content-form"))
                .andExpect(model().attribute("message", "Content saved successfully to single file"))
                .andExpect(model().attributeExists("contentRequest"));

        verify(githubContentService, times(1)).saveAllContentsToSingleFile("https://github.com/user/repo");
        verify(directoryContentService, never()).saveAllContentsToFile(anyString());
    }

    // Остальные тесты аналогичны первому подходу, но без редиректа
}
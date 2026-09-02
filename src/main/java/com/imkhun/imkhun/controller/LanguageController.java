package com.imkhun.imkhun.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LanguageController {

    @GetMapping("/api/learning")
    public List<String> getLearningLanguages() {
        return List.of("일본어", "영어");
    }

    @GetMapping("/api/teaching")
    public List<String> getTeachingLanguages() {
        return List.of("태국어", "한국어");
    }
}
package com.campusqa.demo.controller;

import com.campusqa.demo.model.KnowledgeDocument;
import com.campusqa.demo.service.KnowledgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public List<KnowledgeDocument> list(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String category) {
        return knowledgeService.list(keyword, category);
    }

    @GetMapping("/{id}")
    public KnowledgeDocument getById(@PathVariable String id) {
        return knowledgeService.getById(id);
    }
}
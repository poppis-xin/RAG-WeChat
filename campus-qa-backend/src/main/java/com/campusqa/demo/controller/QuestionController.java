package com.campusqa.demo.controller;

import com.campusqa.demo.dto.AiReplyResponse;
import com.campusqa.demo.dto.AskAiRequest;
import com.campusqa.demo.dto.CreateQuestionRequest;
import com.campusqa.demo.model.Question;
import com.campusqa.demo.service.AiService;
import com.campusqa.demo.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionService questionService;
    private final AiService aiService;

    public QuestionController(QuestionService questionService, AiService aiService) {
        this.questionService = questionService;
        this.aiService = aiService;
    }

    @GetMapping("/questions")
    public List<Question> listQuestions(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String category) {
        return questionService.list(keyword, category);
    }

    @GetMapping("/questions/{id}")
    public Question getQuestion(@PathVariable String id) {
        return questionService.getById(id);
    }

    @PostMapping("/questions")
    public Question createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        return questionService.create(request);
    }

    @DeleteMapping("/questions/{id}")
    public Map<String, Object> deleteQuestion(@PathVariable String id,
                                              @RequestParam(required = false) String author) {
        questionService.delete(id, author);
        return Map.of("success", true);
    }

    @PostMapping("/ai/answer")
    public AiReplyResponse askAi(@Valid @RequestBody AskAiRequest request) {
        return aiService.ask(request.getQuestion());
    }

    @PostMapping(value = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAi(@Valid @RequestBody AskAiRequest request) {
        return aiService.streamAsk(request.getQuestion());
    }

    @GetMapping("/meta")
    public Map<String, Object> getMeta() {
        return Map.of(
                "categories", questionService.getCategories(),
                "tags", questionService.getTags()
        );
    }
}
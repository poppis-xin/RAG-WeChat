package com.campusqa.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AskAiRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 200, message = "问题长度不能超过 200 个字符")
    private String question;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}

package com.campusqa.demo.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApiErrorResponse {

    private String code;
    private String message;
    private String timestamp;
    private List<String> details = new ArrayList<>();

    public ApiErrorResponse() {
        this.timestamp = LocalDateTime.now().toString();
    }

    public ApiErrorResponse(String code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public ApiErrorResponse(String code, String message, List<String> details) {
        this(code, message);
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
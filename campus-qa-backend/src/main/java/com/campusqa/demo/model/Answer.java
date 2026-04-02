package com.campusqa.demo.model;

public class Answer {

    private String id;
    private String authorName;
    private String content;
    private boolean accepted;
    private String createdAt;

    public Answer() {
    }

    public Answer(String id, String authorName, String content, boolean accepted, String createdAt) {
        this.id = id;
        this.authorName = authorName;
        this.content = content;
        this.accepted = accepted;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

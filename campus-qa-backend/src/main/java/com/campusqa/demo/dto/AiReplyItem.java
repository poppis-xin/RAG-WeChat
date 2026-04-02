package com.campusqa.demo.dto;

public class AiReplyItem {

    private String id;
    private String title;
    private String category;

    public AiReplyItem() {
    }

    public AiReplyItem(String id, String title, String category) {
        this.id = id;
        this.title = title;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

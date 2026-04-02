package com.campusqa.demo.model;

import java.util.List;

public class AiAnswer {

    private String summary;
    private List<String> refs;

    public AiAnswer() {
    }

    public AiAnswer(String summary, List<String> refs) {
        this.summary = summary;
        this.refs = refs;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getRefs() {
        return refs;
    }

    public void setRefs(List<String> refs) {
        this.refs = refs;
    }
}

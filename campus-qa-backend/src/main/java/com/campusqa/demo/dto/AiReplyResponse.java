package com.campusqa.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class AiReplyResponse {

    private String answer;
    private List<String> refs = new ArrayList<>();
    private List<String> officialRefs = new ArrayList<>();
    private List<String> studentExperiences = new ArrayList<>();
    private List<AiReplyItem> matchedQuestions = new ArrayList<>();
    private boolean remote;
    private String mode;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getRefs() {
        return refs;
    }

    public void setRefs(List<String> refs) {
        this.refs = refs;
    }

    public List<String> getOfficialRefs() {
        return officialRefs;
    }

    public void setOfficialRefs(List<String> officialRefs) {
        this.officialRefs = officialRefs;
    }

    public List<String> getStudentExperiences() {
        return studentExperiences;
    }

    public void setStudentExperiences(List<String> studentExperiences) {
        this.studentExperiences = studentExperiences;
    }

    public List<AiReplyItem> getMatchedQuestions() {
        return matchedQuestions;
    }

    public void setMatchedQuestions(List<AiReplyItem> matchedQuestions) {
        this.matchedQuestions = matchedQuestions;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
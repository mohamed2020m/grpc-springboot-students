package com.leeuw.dto;

public class CustomResponse {
    private Student student;
    private String message;

    public CustomResponse(Student student, String message) {
    }


    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


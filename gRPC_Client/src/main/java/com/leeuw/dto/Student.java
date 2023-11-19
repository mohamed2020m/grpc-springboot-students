package com.leeuw.dto;

public class Student {
    private Long id;
    private String firstName;
    private String lastName;
    private Long age;

    public Student() {
    }

    public Student(String firstName, String lastName, Long age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getAge() {
        return this.age;
    }

    public void setAge(Long age) {
        this.age = age;
    }

}


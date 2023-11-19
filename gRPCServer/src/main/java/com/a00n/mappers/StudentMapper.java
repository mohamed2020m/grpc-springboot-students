package com.a00n.mappers;

import org.springframework.stereotype.Component;

import com.a00n.entities.Student;

@Component
public class StudentMapper {
    public com.a00n.grpc.stubs.StudentOuterClass.Student toGrpcStudent(Student student) {
        return com.a00n.grpc.stubs.StudentOuterClass.Student.newBuilder().setId(student.getId())
                .setFirstName(student.getFirstName())
                .setLastName(student.getLastName())
                .setAge(student.getAge())
                .build();
    }

    public Student fromGrpcStudent(com.a00n.grpc.stubs.StudentOuterClass.Student student) {
        return Student.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .age(student.getAge())
                .build();
    }
}

package com.a00n.grpc.services;

import com.a00n.entities.Student;
import com.a00n.grpc.stubs.StudentOuterClass;
import com.a00n.grpc.stubs.StudentOuterClass.*;
import com.a00n.grpc.stubs.StudentServiceGrpc;
import com.a00n.mappers.StudentMapper;
import com.a00n.repositories.StudentRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

@GrpcService
@RequiredArgsConstructor
public class GrpcStudentServiceIml extends StudentServiceGrpc.StudentServiceImplBase {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Override
    public void listStudents(Empty request, StreamObserver<ListStudentsResponse> responseObserver) {
        List<Student> students = studentRepository.findAll();
        List<StudentOuterClass.Student> listStudents = students.stream()
                .map(studentMapper::toGrpcStudent)
                .toList();
        ListStudentsResponse listStudentsResponse = ListStudentsResponse.newBuilder()
                .addAllStudents(listStudents)
                .build();
        responseObserver.onNext(listStudentsResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void listStudentsStream(Empty request,
                                   StreamObserver<StudentOuterClass.Student> responseObserver) {
        List<Student> students = studentRepository.findAll();
        List<StudentOuterClass.Student> listStudents = students.stream()
                .map(studentMapper::toGrpcStudent)
                .toList();
        if (listStudents.isEmpty()) {
            responseObserver.onError(Status.INTERNAL.withDescription("no student found").asException());
        } else {
            Stack<StudentOuterClass.Student> stackStudents = new Stack<>();
            stackStudents.addAll(listStudents);
            Timer timer = new Timer("students timer");
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    responseObserver.onNext(stackStudents.pop());
                    if (stackStudents.isEmpty()) {
                        responseObserver.onCompleted();
                        timer.cancel();
                    }
                }

            }, 0, 1000);
        }
    }

    @Override
    public void getStudent(GetStudentRequest request,
                           StreamObserver<StudentOuterClass.Student> responseObserver) {
        Student student = studentRepository.findById(request.getId()).orElse(null);
        if (student == null) {
            responseObserver.onError(Status.INTERNAL.withDescription("student not found").asException());
        } else {
            responseObserver.onNext(studentMapper.toGrpcStudent(student));
            responseObserver.onCompleted();
        }
    }

    @Override
    public void createStudent(CreateStudentRequest request,
                              StreamObserver<StudentOuterClass.Student> responseObserver) {
        Student student = Student.builder().firstName(request.getFirstName()).lastName(request.getLastName()).age(request.getAge()).build();
        responseObserver.onNext(studentMapper.toGrpcStudent(studentRepository.save(student)));
        responseObserver.onCompleted();
    }

    @Override
    public void updateStudent(StudentOuterClass.Student request,
                              StreamObserver<StudentOuterClass.Student> responseObserver) {
        if (studentRepository.existsById(request.getId())) {
            Student student = studentRepository.save(studentMapper.fromGrpcStudent(request));
            responseObserver.onNext(studentMapper.toGrpcStudent(student));
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.INTERNAL.withDescription("student not found").asException());
        }
    }

    @Override
    public void deleteStudent(DeleteStudentRequest request,
                              StreamObserver<DeleteStudentResponse> responseObserver) {
        if (studentRepository.existsById(request.getId())) {
            studentRepository.deleteById(request.getId());
            DeleteStudentResponse deleteStudentResponse = DeleteStudentResponse.newBuilder()
                    .setMessage("Student Deleted").build();
            responseObserver.onNext(deleteStudentResponse);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.INTERNAL.withDescription("student not found").asException());
        }
    }
}

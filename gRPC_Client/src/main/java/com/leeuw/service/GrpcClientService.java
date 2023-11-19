package com.leeuw.service;

import com.google.protobuf.Empty;
import com.leeuw.grpc.stubs.StudentOuterClass;
import com.leeuw.grpc.stubs.StudentServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class GrpcClientService {
    @GrpcClient("service")
    StudentServiceGrpc.StudentServiceBlockingStub studentServiceStub;

    @GrpcClient("service")
    StudentServiceGrpc.StudentServiceStub asyncStudentServiceStub;
    public StudentOuterClass.ListStudentsResponse listStudents() {
        return studentServiceStub.listStudents(StudentOuterClass.Empty.newBuilder().build());
    }

    public Flux<StudentOuterClass.Student> listStudentsStream() {
        // Use Flux.create to handle the asynchronous nature of gRPC streaming
        return Flux.create(emitter -> {
            asyncStudentServiceStub.listStudentsStream(StudentOuterClass.Empty.newBuilder().build(),
                new StreamObserver<StudentOuterClass.Student>() {
                    @Override
                    public void onNext(StudentOuterClass.Student student) {
                        // Emit each student to the Flux
                        emitter.next(student);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        // Signal error to the Flux
                        emitter.error(throwable);
                    }

                    @Override
                    public void onCompleted() {
                        // Signal completion to the Flux
                        emitter.complete();
                    }
                });
        }, FluxSink.OverflowStrategy.BUFFER);
    }




    public StudentOuterClass.Student getStudentById(long id) {
        StudentOuterClass.GetStudentRequest request = StudentOuterClass.GetStudentRequest.newBuilder().setId(id).build();
        return studentServiceStub.getStudent(request);
    }

    public StudentOuterClass.Student createStudent(String firstName, String lastName, long age) {
        StudentOuterClass.CreateStudentRequest request = StudentOuterClass.CreateStudentRequest.newBuilder()
            .setFirstName(firstName)
            .setLastName(lastName)
            .setAge(age)
            .build();

        return studentServiceStub.createStudent(request);
    }

    public StudentOuterClass.Student updateStudent(StudentOuterClass.Student student) {
        return studentServiceStub.updateStudent(student);
    }

    public StudentOuterClass.DeleteStudentResponse deleteStudent(long id) {
        StudentOuterClass.DeleteStudentRequest request = StudentOuterClass.DeleteStudentRequest.newBuilder().setId(id).build();
        return studentServiceStub.deleteStudent(request);
    }
}


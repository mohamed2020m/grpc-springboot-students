package com.leeuw.controller;

import com.leeuw.dto.CustomResponse;
import com.leeuw.dto.Student;
import com.leeuw.grpc.stubs.StudentOuterClass;
import com.leeuw.service.GrpcClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/students")
public class GrpcController {

    private GrpcClientService grpcStudentClient;

    @Autowired
    public GrpcController(GrpcClientService grpcStudentClient) {
        this.grpcStudentClient = grpcStudentClient;
    }

    @GetMapping
    public ResponseEntity<List<Student>> getStudentList() {
        try {
            // Call gRPC service to get a list of students
            StudentOuterClass.ListStudentsResponse studentList = grpcStudentClient.listStudents();

            // Convert gRPC response to DTOs
            List<Student> responseList = new ArrayList<>();
            for (StudentOuterClass.Student student : studentList.getStudentsList()) {
                Student studentDTO = new Student();
                studentDTO.setId(student.getId());
                studentDTO.setFirstName(student.getFirstName());
                studentDTO.setLastName(student.getLastName());
                studentDTO.setAge(student.getAge());

                responseList.add(studentDTO);
            }

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            // Handle the exception, you can log it or return a specific error response
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping(value = "/{id}")
        public ResponseEntity<Student> getStudentById(@PathVariable Long id) {

        try{
            StudentOuterClass.Student student = grpcStudentClient.getStudentById(id);
            // Convert gRPC response to DTO
            Student createdStudentDTO = new Student();
            createdStudentDTO.setId(student.getId());
            createdStudentDTO.setFirstName(student.getFirstName());
            createdStudentDTO.setLastName(student.getLastName());
            createdStudentDTO.setAge(student.getAge());
            return ResponseEntity.ok(createdStudentDTO);

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Student> streamStudents() {
        return Flux.fromStream(grpcStudentClient.listStudentsStream().toStream())
            .map(student -> {
                Student studentDTO = new Student();
                studentDTO.setId(student.getId());
                studentDTO.setFirstName(student.getFirstName());
                studentDTO.setLastName(student.getLastName());
                studentDTO.setAge(student.getAge());
                return studentDTO;
            });

//        return Flux.interval(Duration.ofSeconds(5))
//                .publishOn(Schedulers.boundedElastic())
//                .flatMap(sequence -> {
//                    // Convert the gRPC stream to a Flux
//                    Flux<StudentOuterClass.Student> grpcStudentStream = Flux.fromStream(grpcStudentClient.listStudentsStream().toStream());
//
//                    // Map each gRPC student to a Student DTO
//                    return grpcStudentStream.map(student -> {
//                        Student studentDTO = new Student();
//                        studentDTO.setId(student.getId());
//                        studentDTO.setFirstName(student.getFirstNname());
//                        studentDTO.setLastName(student.getLastName());
//                        studentDTO.setAge(student.getAge());
//                        return studentDTO;
//                    });
//                });
    }


    @PostMapping
    public ResponseEntity<Student> createStudent(@RequestBody Student request) {
        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        long age = request.getAge();
        System.out.println(firstName);
        StudentOuterClass.Student createdStudent = grpcStudentClient.createStudent(firstName, lastName, age);

        // Convert gRPC response to DTO
        Student createdStudentDTO = new Student();
        createdStudentDTO.setId(createdStudent.getId());
        createdStudentDTO.setFirstName(createdStudent.getFirstName());
        createdStudentDTO.setLastName(createdStudent.getLastName());
        createdStudentDTO.setAge(createdStudent.getAge());

        return ResponseEntity.ok(createdStudentDTO);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student updatedStudentDTO) {
        StudentOuterClass.Student updatedStudent = StudentOuterClass.Student.newBuilder()
            .setId(id)
            .setFirstName(updatedStudentDTO.getFirstName())
            .setLastName(updatedStudentDTO.getLastName())
            .setAge(updatedStudentDTO.getAge())
            .build();

        // Call gRPC service to update student
        StudentOuterClass.Student updatedStudentResponse = grpcStudentClient.updateStudent(updatedStudent);

        // Convert gRPC response to DTO
        Student updatedStudentResponseDTO = new Student();
        updatedStudentResponseDTO.setId(updatedStudentResponse.getId());
        updatedStudentResponseDTO.setFirstName(updatedStudentResponse.getFirstName());
        updatedStudentResponseDTO.setLastName(updatedStudentResponse.getLastName());
        updatedStudentResponseDTO.setAge(updatedStudentResponse.getAge());

        return ResponseEntity.ok(updatedStudentResponseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable Long id) {
        // Call gRPC service to delete student
        StudentOuterClass.DeleteStudentResponse deleteResponse = grpcStudentClient.deleteStudent(id);

        // Check if the deletion was successful
        if (deleteResponse != null && "Student Deleted".equalsIgnoreCase(deleteResponse.getMessage())) {
            return ResponseEntity.ok("Student deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete student.");
        }
    }

}

package com.a00n.grpcstudentserver;

import com.a00n.entities.Student;
import com.a00n.repositories.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.a00n.entities"})
@ComponentScan(basePackages = {
        "com.a00n.grpc.services",
        "com.a00n.mappers",
        "com.a00n.repositories",
        "com.a00n.grpc.interceptors"
})
@EnableJpaRepositories(basePackages = {"com.a00n.repositories"})
public class GrpcStudentServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcStudentServerApplication.class, args);
    }

    @Bean
    public CommandLineRunner startup(StudentRepository studentRepository) {
        return args -> {
            for (int i = 1; i <= 10; i++) {
                studentRepository.save(Student.builder().firstName("ayoub " + i).lastName("nouri " + i).age((long) (i * 10)).build());
            }
        };
    }
}

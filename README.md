# gRPC with Spring Boot ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)

In this project, we will be developing a gRPC Spring Boot application with both server and client components. The primary objective is to implement CRUD (Create, Read, Update, Delete) operations for managing student records. The application will enable functionalities such as adding new students, deleting existing records, updating student information, and retrieving a list of students
We will also leverage React programming to efficiently retrieve and display a list of students using streams.

## Server Setup

Follow these steps to set up the server:

### Create a spring boot project

Use spring initializr (or anything you want) and create a simple spring boot project

![initialise_spring_boot_app.png](assets/initialise_server_app.png)

### Add maven dependencies

Include the following properties within your configuration:

```xml
<protobuf.version>3.23.4</protobuf.version>
<protobuf-plugin.version>0.6.1</protobuf-plugin.version>
<grpc.version>1.58.0</grpc.version>
```

Adding the following dependencies:

```xml
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>${grpc.version}</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>${grpc.version}</version>
</dependency>
<dependency>
    <groupId>jakarta.annotation</groupId>
    <artifactId>jakarta.annotation-api</artifactId>
    <version>1.3.5</version>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>2.15.0.RELEASE</version>
</dependency>
```

Add the following plugin:

```xml
<plugin>
    <groupId>com.github.os72</groupId>
    <artifactId>protoc-jar-maven-plugin</artifactId>
    <version>3.11.4</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <includeMavenTypes>direct</includeMavenTypes>
                <inputDirectories>
                    <include>src/main/resources</include>
                </inputDirectories>
                <outputTargets>
                    <outputTarget>
                        <type>java</type>
                        <outputDirectory>src/main/java</outputDirectory>
                    </outputTarget>
                    <outputTarget>
                        <type>grpc-java</type>
                        <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.15.0</pluginArtifact>
                        <outputDirectory>src/main/java</outputDirectory>
                    </outputTarget>
                </outputTargets>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Creating the gRPC-Service Definitions

Place your protobuf definitions / `.proto` files in `src/main/resources`. For writing protobuf files please refer to the official [protobuf docs](https://developers.google.com/protocol-buffers/docs/proto3).

Your `.proto` files will look similar to the example below:

```protobuf
syntax = "proto3";
option java_package = "com.a00n.grpc.stubs"; // replace this with your_package_name_gprc.stubs 

message Student {
  int64 id = 1;
  string firstName = 2;
  string lastName = 3;
  int64 age = 4;
}
message Empty {}

service StudentService {
  rpc ListStudents(Empty) returns (ListStudentsResponse);
  rpc GetStudent(GetStudentRequest) returns (Student);
  rpc CreateStudent(CreateStudentRequest) returns (Student);
  rpc UpdateStudent(Student) returns (Student);
  rpc DeleteStudent(DeleteStudentRequest) returns (DeleteStudentResponse);
}

message ListStudentsResponse { repeated Student students = 1; }
message GetStudentRequest { int64 id = 1; }
message DeleteStudentRequest { int64 id = 1; }
message DeleteStudentResponse { string message = 1; }
message CreateStudentRequest {
  string firstName = 1;
  string lastName = 2;
  int64 age = 3;
}
```

- run `mvn clean install`

- Please observe that the package `grpc.studs` has been generated. Inside this package, you should find two Java files: `StudentOutClass` and `StudentServiceGrpc`

### Create a student jpa entity

First let's create our jpa entity :

```java
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    private Long age;

}
```

also as always you should create the repositories :

```java
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
}
```

### Mapping Grpc Student with Jpa Student

Now let's create a class that will map objects of Grpc student class to objects of jpa student class :

```java
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
        return new Student(student.getId(), student.getFirstName(), student.getLastName(), student.getAge());
    }
}
```

### Implementing the Service

The `protoc-jar-maven-plugin` plugin generates a class for each of your grpc services. For example: `MyServiceGrpc` where `MyService` is the name of the grpc service in the proto file. This class contains both the client stubs and the server `ImplBase` that you will need to extend.

After that you have only four tasks to do:

1. Make sure that your `MyServiceImpl` extends `MyServiceGrpc.MyServiceImplBase`
2. Add the `@GrpcService` annotation to your `MyServiceImpl` class
3. Make sure that the `MyServiceImpl` is added to your application context,
   - either by creating `@Bean` definition in one of your `@Configuration` classes
   - or placing it in spring’s automatically detected paths (e.g. in the same or a sub package of your `Main` class)
4. Actually implement the grpc service methods.

Your grpc service class will then look somewhat similar to the example below:

```java
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
```

That’s all there is to that. Now you can start your spring-boot application and start sending requests to your grpc-service.

By default, the grpc-server will be started on port `9090` using `PLAINTEXT` mode.
You can test that your application is working as expected by using this [BloomRPC](https://github.com/bloomrpc/bloomrpc/releases) :

![initialise_spring_boot_app.png](assets/bloomrpc1.png)

## Client Setup

Follow these steps to set up the client:

### Create a spring boot project

Head to https://start.spring.io and generate a spring boot app

![initialise_spring_boot_app.png](assets%2Finitialise_spring_boot_app.png)

### Add maven dependencies

Include the following properties within your configuration:

```xml
<protobuf.version>3.23.4</protobuf.version>
<protobuf-plugin.version>0.6.1</protobuf-plugin.version>
<grpc.version>1.58.0</grpc.version>
```

Adding the following dependencies:

```xml
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>${grpc.version}</version>
</dependency>

<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>${grpc.version}</version>
</dependency>

<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-client-spring-boot-starter</artifactId>
    <version>2.15.0.RELEASE</version>
</dependency>

<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.2</version>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
    <version>3.5.11</version>
</dependency>

<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
</dependency>
```

Add the following plugin:

```xml
<plugin>
    <groupId>com.github.os72</groupId>
    <artifactId>protoc-jar-maven-plugin</artifactId>
    <version>3.11.4</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <includeMavenTypes>direct</includeMavenTypes>
                <inputDirectories>
                    <include>src/main/resources</include>
                </inputDirectories>
                <outputTargets>
                    <outputTarget>
                        <type>java</type>
                        <outputDirectory>src/main/java</outputDirectory>
                    </outputTarget>
                    <outputTarget>
                        <type>grpc-java</type>
                        <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.15.0</pluginArtifact>
                        <outputDirectory>src/main/java</outputDirectory>
                    </outputTarget>
                </outputTargets>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Generate stubs

- Make sure to have the `Student.proto` inside the resources folder.

- run `mvn clean install`

- Please observe that the package `grpc.studs` has been generated. Inside this package, you should find two Java files: `StudentOutClass` and `StudentServiceGrpc`

### Create a dto

Create a Student dto.

```java
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
```

### Create a student client service

Create the client Service

```java

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
            .setFirstNname(firstName)
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
```

### Create a student controller

Creating a controller name it `GrpcController` or what ever you want.

```java
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
                studentDTO.setFirstName(student.getFirstNname());
                studentDTO.setLastName(student.getLastName());
                studentDTO.setAge(student.getAge());

                responseList.add(studentDTO);
            }

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            // Handle the exception, you can log it or return a specific error response
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
            createdStudentDTO.setFirstName(student.getFirstNname());
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
                studentDTO.setFirstName(student.getFirstNname());
                studentDTO.setLastName(student.getLastName());
                studentDTO.setAge(student.getAge());
                return studentDTO;
            });
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
        createdStudentDTO.setFirstName(createdStudent.getFirstNname());
        createdStudentDTO.setLastName(createdStudent.getLastName());
        createdStudentDTO.setAge(createdStudent.getAge());

        return ResponseEntity.ok(createdStudentDTO);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student updatedStudentDTO) {
        StudentOuterClass.Student updatedStudent = StudentOuterClass.Student.newBuilder()
            .setId(id)
            .setFirstNname(updatedStudentDTO.getFirstName())
            .setLastName(updatedStudentDTO.getLastName())
            .setAge(updatedStudentDTO.getAge())
            .build();

        // Call gRPC service to update student
        StudentOuterClass.Student updatedStudentResponse = grpcStudentClient.updateStudent(updatedStudent);

        // Convert gRPC response to DTO
        Student updatedStudentResponseDTO = new Student();
        updatedStudentResponseDTO.setId(updatedStudentResponse.getId());
        updatedStudentResponseDTO.setFirstName(updatedStudentResponse.getFirstNname());
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
```

### Config the app

Add the following lines to `application.properties`

```properties
grpc.client.service.address=static://localhost:9090
grpc.client.service.negotiation-type=plaintext
```

### Test With postman

#### Create a Student

![create_student.png](assets%2Fcreate_student.png)

#### List all Students

![get_all_students.png](assets%2Fget_all_students.png)

#### List a Student

![get_a_student.png](assets%2Fget_a_student.png)

#### Update a Student

![update_student.png](assets%2Fupdate_student.png)

#### Delete a Student

![delete_student.png](assets%2Fdelete_student.png)

#### Retrieve a list of students using streams.

![get_student_streams.png](assets%2Fget_student_streams.png)

## References

https://yidongnan.github.io/grpc-spring-boot-starter/en/server/getting-started.html

## Authors

- [Leeuw](https://github.com/mohamed2020m)
- [A00N](https://github.com/Ay0ubN0uri)

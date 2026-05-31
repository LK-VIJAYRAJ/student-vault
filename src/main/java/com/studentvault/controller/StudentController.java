package com.studentvault.controller;

import com.studentvault.dto.StudentDTO;
import com.studentvault.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<Page<StudentDTO.Summary>> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Page<StudentDTO.Summary> students = studentService.getAllStudents(
                PageRequest.of(page, size, Sort.by(sortBy)));
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO.Response> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/roll/{rollNumber}")
    public ResponseEntity<StudentDTO.Response> getStudentByRollNumber(@PathVariable String rollNumber) {
        return ResponseEntity.ok(studentService.getStudentByRollNumber(rollNumber));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<StudentDTO.Summary>> searchStudents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer semester,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                studentService.searchStudents(name, departmentId, semester,
                        PageRequest.of(page, size)));
    }

    @GetMapping("/by-dept-semester")
    public ResponseEntity<Page<StudentDTO.Summary>> getByDeptAndSemester(
            @RequestParam Long departmentId,
            @RequestParam Integer semester,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                studentService.getStudentsByDepartmentAndSemester(departmentId, semester,
                        PageRequest.of(page, size)));
    }

    @PostMapping
    public ResponseEntity<StudentDTO.Response> createStudent(
            @Valid @RequestBody StudentDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentService.createStudent(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO.Response> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentDTO.Request request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}

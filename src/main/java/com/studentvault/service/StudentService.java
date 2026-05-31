package com.studentvault.service;

import com.studentvault.dto.StudentDTO;
import com.studentvault.exception.ResourceNotFoundException;
import com.studentvault.exception.DuplicateResourceException;
import com.studentvault.model.Department;
import com.studentvault.model.Student;
import com.studentvault.repository.DepartmentRepository;
import com.studentvault.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public Page<StudentDTO.Summary> getAllStudents(Pageable pageable) {
        log.info("Fetching students page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return studentRepository.findAll(pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public StudentDTO.Response getStudentById(Long id) {
        Student student = findStudentOrThrow(id);
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public StudentDTO.Response getStudentByRollNumber(String rollNumber) {
        // This query uses idx_students_roll_number — O(log N)
        Student student = studentRepository.findByRollNumber(rollNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with roll number: " + rollNumber));
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentDTO.Summary> searchStudents(String name, Long departmentId,
                                                    Integer semester, Pageable pageable) {
        return studentRepository.searchStudents(name, departmentId, semester, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<StudentDTO.Summary> getStudentsByDepartmentAndSemester(Long departmentId,
                                                                        Integer semester,
                                                                        Pageable pageable) {
        // Uses composite index idx_students_dept_semester
        return studentRepository.findByDepartmentIdAndSemester(departmentId, semester, pageable)
                .map(this::toSummary);
    }

    @Transactional
    public StudentDTO.Response createStudent(StudentDTO.Request request) {
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new DuplicateResourceException("Roll number already exists: " + request.getRollNumber());
        }
        Department department = findDepartmentOrThrow(request.getDepartmentId());
        Student student = Student.builder()
                .name(request.getName())
                .email(request.getEmail())
                .rollNumber(request.getRollNumber())
                .department(department)
                .semester(request.getSemester())
                .dateOfBirth(request.getDateOfBirth())
                .phone(request.getPhone())
                .build();
        Student saved = studentRepository.save(student);
        log.info("Created student id={} rollNumber={}", saved.getId(), saved.getRollNumber());
        return toResponse(saved);
    }

    @Transactional
    public StudentDTO.Response updateStudent(Long id, StudentDTO.Request request) {
        Student student = findStudentOrThrow(id);
        // If email changed, check uniqueness
        if (!student.getEmail().equals(request.getEmail()) &&
                studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }
        Department department = findDepartmentOrThrow(request.getDepartmentId());
        student.setName(request.getName());
        student.setEmail(request.getEmail());
        student.setDepartment(department);
        student.setSemester(request.getSemester());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setPhone(request.getPhone());
        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
        log.info("Deleted student id={}", id);
    }

    // --- Mapping helpers ---

    private Student findStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    private Department findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    public StudentDTO.Response toResponse(Student s) {
        return StudentDTO.Response.builder()
                .id(s.getId())
                .name(s.getName())
                .email(s.getEmail())
                .rollNumber(s.getRollNumber())
                .departmentName(s.getDepartment().getName())
                .departmentCode(s.getDepartment().getCode())
                .semester(s.getSemester())
                .dateOfBirth(s.getDateOfBirth())
                .phone(s.getPhone())
                .build();
    }

    private StudentDTO.Summary toSummary(Student s) {
        return StudentDTO.Summary.builder()
                .id(s.getId())
                .name(s.getName())
                .rollNumber(s.getRollNumber())
                .departmentCode(s.getDepartment().getCode())
                .semester(s.getSemester())
                .build();
    }
}

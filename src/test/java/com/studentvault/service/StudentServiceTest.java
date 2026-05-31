package com.studentvault.service;

import com.studentvault.dto.StudentDTO;
import com.studentvault.exception.DuplicateResourceException;
import com.studentvault.exception.ResourceNotFoundException;
import com.studentvault.model.Department;
import com.studentvault.model.Student;
import com.studentvault.repository.DepartmentRepository;
import com.studentvault.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService — Unit Tests")
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private StudentService studentService;

    private Department department;
    private Student student;
    private StudentDTO.Request validRequest;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .name("Computer Science")
                .code("CSE")
                .build();

        student = Student.builder()
                .id(1L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .rollNumber("CSE000001")
                .department(department)
                .semester(3)
                .dateOfBirth(LocalDate.of(2002, 5, 15))
                .phone("9876543210")
                .build();

        validRequest = StudentDTO.Request.builder()
                .name("Alice Johnson")
                .email("alice@example.com")
                .rollNumber("CSE000001")
                .departmentId(1L)
                .semester(3)
                .dateOfBirth(LocalDate.of(2002, 5, 15))
                .phone("9876543210")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET ALL
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllStudents — returns paginated summaries")
    void getAllStudents_returnsPaginatedSummaries() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Student> page = new PageImpl<>(List.of(student));
        given(studentRepository.findAll(pageable)).willReturn(page);

        Page<StudentDTO.Summary> result = studentService.getAllStudents(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRollNumber()).isEqualTo("CSE000001");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStudentById — returns student when found")
    void getStudentById_returnsStudentWhenFound() {
        given(studentRepository.findById(1L)).willReturn(Optional.of(student));

        StudentDTO.Response response = studentService.getStudentById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice Johnson");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getDepartmentCode()).isEqualTo("CSE");
        assertThat(response.getSemester()).isEqualTo(3);
    }

    @Test
    @DisplayName("getStudentById — throws ResourceNotFoundException when not found")
    void getStudentById_throwsWhenNotFound() {
        given(studentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET BY ROLL NUMBER
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStudentByRollNumber — returns student (tests idx_students_roll_number path)")
    void getStudentByRollNumber_returnsStudent() {
        given(studentRepository.findByRollNumber("CSE000001")).willReturn(Optional.of(student));

        StudentDTO.Response response = studentService.getStudentByRollNumber("CSE000001");

        assertThat(response.getRollNumber()).isEqualTo("CSE000001");
        then(studentRepository).should().findByRollNumber("CSE000001");
    }

    @Test
    @DisplayName("getStudentByRollNumber — throws ResourceNotFoundException for unknown roll number")
    void getStudentByRollNumber_throwsForUnknownRoll() {
        given(studentRepository.findByRollNumber("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentByRollNumber("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createStudent — persists and returns response on success")
    void createStudent_persistsAndReturnsResponse() {
        given(studentRepository.existsByEmail("alice@example.com")).willReturn(false);
        given(studentRepository.existsByRollNumber("CSE000001")).willReturn(false);
        given(departmentRepository.findById(1L)).willReturn(Optional.of(department));
        given(studentRepository.save(any(Student.class))).willReturn(student);

        StudentDTO.Response response = studentService.createStudent(validRequest);

        assertThat(response.getName()).isEqualTo("Alice Johnson");
        assertThat(response.getRollNumber()).isEqualTo("CSE000001");
        then(studentRepository).should().save(any(Student.class));
    }

    @Test
    @DisplayName("createStudent — throws DuplicateResourceException on duplicate email")
    void createStudent_throwsOnDuplicateEmail() {
        given(studentRepository.existsByEmail("alice@example.com")).willReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("alice@example.com");

        then(studentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createStudent — throws DuplicateResourceException on duplicate roll number")
    void createStudent_throwsOnDuplicateRollNumber() {
        given(studentRepository.existsByEmail("alice@example.com")).willReturn(false);
        given(studentRepository.existsByRollNumber("CSE000001")).willReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CSE000001");

        then(studentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createStudent — throws ResourceNotFoundException when department not found")
    void createStudent_throwsWhenDepartmentNotFound() {
        given(studentRepository.existsByEmail(anyString())).willReturn(false);
        given(studentRepository.existsByRollNumber(anyString())).willReturn(false);
        given(departmentRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.createStudent(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStudent — updates fields and returns updated response")
    void updateStudent_updatesAndReturns() {
        StudentDTO.Request updateRequest = StudentDTO.Request.builder()
                .name("Alice Smith")
                .email("alice@example.com") // same email — no uniqueness check
                .rollNumber("CSE000001")
                .departmentId(1L)
                .semester(4)
                .build();

        given(studentRepository.findById(1L)).willReturn(Optional.of(student));
        given(departmentRepository.findById(1L)).willReturn(Optional.of(department));
        given(studentRepository.save(any(Student.class))).willAnswer(inv -> {
            Student s = inv.getArgument(0);
            return s; // return the updated student
        });

        StudentDTO.Response response = studentService.updateStudent(1L, updateRequest);

        assertThat(response.getName()).isEqualTo("Alice Smith");
        assertThat(response.getSemester()).isEqualTo(4);
    }

    @Test
    @DisplayName("updateStudent — throws when student not found")
    void updateStudent_throwsWhenNotFound() {
        given(studentRepository.findById(42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.updateStudent(42L, validRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteStudent — deletes when student exists")
    void deleteStudent_deletesWhenExists() {
        given(studentRepository.existsById(1L)).willReturn(true);
        willDoNothing().given(studentRepository).deleteById(1L);

        assertThatCode(() -> studentService.deleteStudent(1L)).doesNotThrowAnyException();

        then(studentRepository).should().deleteById(1L);
    }

    @Test
    @DisplayName("deleteStudent — throws ResourceNotFoundException when not found")
    void deleteStudent_throwsWhenNotFound() {
        given(studentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> studentService.deleteStudent(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        then(studentRepository).should(never()).deleteById(any());
    }
}

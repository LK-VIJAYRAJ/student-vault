package com.studentvault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentvault.dto.StudentDTO;
import com.studentvault.exception.DuplicateResourceException;
import com.studentvault.exception.ResourceNotFoundException;
import com.studentvault.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@DisplayName("StudentController — WebMvcTest Slice Tests")
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentService studentService;

    // ─────────────────────────────────────────────────────────────────────────
    //  GET ALL — /api/students
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/students — returns 200 with page of students")
    void getAllStudents_returns200() throws Exception {
        StudentDTO.Summary summary = StudentDTO.Summary.builder()
                .id(1L)
                .name("Alice Johnson")
                .rollNumber("CSE000001")
                .departmentCode("CSE")
                .semester(3)
                .build();
        Page<StudentDTO.Summary> page = new PageImpl<>(List.of(summary));
        given(studentService.getAllStudents(any())).willReturn(page);

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rollNumber").value("CSE000001"))
                .andExpect(jsonPath("$.content[0].departmentCode").value("CSE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET BY ID — /api/students/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/students/{id} — returns 200 with student response")
    void getStudentById_returns200() throws Exception {
        StudentDTO.Response response = StudentDTO.Response.builder()
                .id(1L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .rollNumber("CSE000001")
                .departmentName("Computer Science")
                .departmentCode("CSE")
                .semester(3)
                .build();
        given(studentService.getStudentById(1L)).willReturn(response);

        mockMvc.perform(get("/api/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.departmentCode").value("CSE"));
    }

    @Test
    @DisplayName("GET /api/students/{id} — returns 404 when not found")
    void getStudentById_returns404WhenNotFound() throws Exception {
        given(studentService.getStudentById(999L))
                .willThrow(new ResourceNotFoundException("Student not found with id: 999"));

        mockMvc.perform(get("/api/students/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found with id: 999"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET BY ROLL NUMBER — /api/students/roll/{rollNumber}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/students/roll/{roll} — returns 200 for valid roll number")
    void getByRollNumber_returns200() throws Exception {
        StudentDTO.Response response = StudentDTO.Response.builder()
                .id(42L)
                .rollNumber("CSE000042")
                .name("Bob Smith")
                .email("bob@example.com")
                .departmentCode("CSE")
                .semester(5)
                .build();
        given(studentService.getStudentByRollNumber("CSE000042")).willReturn(response);

        mockMvc.perform(get("/api/students/roll/CSE000042"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rollNumber").value("CSE000042"))
                .andExpect(jsonPath("$.name").value("Bob Smith"));
    }

    @Test
    @DisplayName("GET /api/students/roll/{roll} — returns 404 for unknown roll number")
    void getByRollNumber_returns404ForUnknown() throws Exception {
        given(studentService.getStudentByRollNumber("UNKNOWN"))
                .willThrow(new ResourceNotFoundException("Student not found with roll number: UNKNOWN"));

        mockMvc.perform(get("/api/students/roll/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE — POST /api/students
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/students — returns 201 with created student")
    void createStudent_returns201() throws Exception {
        StudentDTO.Request request = StudentDTO.Request.builder()
                .name("Alice Johnson")
                .email("alice@example.com")
                .rollNumber("CSE000001")
                .departmentId(1L)
                .semester(3)
                .build();
        StudentDTO.Response response = StudentDTO.Response.builder()
                .id(1L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .rollNumber("CSE000001")
                .departmentCode("CSE")
                .semester(3)
                .build();
        given(studentService.createStudent(any())).willReturn(response);

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rollNumber").value("CSE000001"));
    }

    @Test
    @DisplayName("POST /api/students — returns 409 when email already exists")
    void createStudent_returns409OnDuplicateEmail() throws Exception {
        StudentDTO.Request request = StudentDTO.Request.builder()
                .name("Alice Johnson")
                .email("duplicate@example.com")
                .rollNumber("CSE000001")
                .departmentId(1L)
                .semester(3)
                .build();
        given(studentService.createStudent(any()))
                .willThrow(new DuplicateResourceException("Email already registered: duplicate@example.com"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/students — returns 400 when request body is invalid")
    void createStudent_returns400OnValidationFailure() throws Exception {
        // Missing required fields — name is blank, semester is null
        StudentDTO.Request invalidRequest = StudentDTO.Request.builder()
                .name("")
                .email("not-an-email")
                .rollNumber("CSE000001")
                .departmentId(1L)
                .semester(null)
                .build();

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE — DELETE /api/students/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/students/{id} — returns 204 on successful delete")
    void deleteStudent_returns204() throws Exception {
        willDoNothing().given(studentService).deleteStudent(1L);

        mockMvc.perform(delete("/api/students/1"))
                .andExpect(status().isNoContent());

        then(studentService).should().deleteStudent(1L);
    }

    @Test
    @DisplayName("DELETE /api/students/{id} — returns 404 when student not found")
    void deleteStudent_returns404WhenNotFound() throws Exception {
        given(studentService.getStudentById(999L))
                .willThrow(new ResourceNotFoundException("Student not found with id: 999"));
        willThrow(new ResourceNotFoundException("Student not found with id: 999"))
                .given(studentService).deleteStudent(999L);

        mockMvc.perform(delete("/api/students/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

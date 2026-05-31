package com.studentvault.service;

import com.studentvault.dto.ResultDTO;
import com.studentvault.exception.DuplicateResourceException;
import com.studentvault.exception.ResourceNotFoundException;
import com.studentvault.model.*;
import com.studentvault.repository.CourseRepository;
import com.studentvault.repository.ResultRepository;
import com.studentvault.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResultService — Unit Tests")
class ResultServiceTest {

    @Mock private ResultRepository  resultRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private CourseRepository  courseRepository;

    @InjectMocks
    private ResultService resultService;

    private Department department;
    private Student    student;
    private Course     course;
    private Result     result;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L).name("Computer Science").code("CSE").build();

        student = Student.builder()
                .id(10L).name("Bob Kumar").email("bob@example.com")
                .rollNumber("CSE000010").department(department).semester(4)
                .build();

        course = Course.builder()
                .id(5L).name("Data Structures").code("CS301")
                .department(department).credits(4).build();

        result = Result.builder()
                .id(100L)
                .student(student)
                .course(course)
                .semester(4)
                .marks(new BigDecimal("87.50"))
                .grade("A")
                .examDate(LocalDate.of(2024, 5, 20))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET RESULTS BY STUDENT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getResultsByStudent — returns list when student exists")
    void getResultsByStudent_returnsListWhenStudentExists() {
        given(studentRepository.existsById(10L)).willReturn(true);
        given(resultRepository.findByStudentId(10L)).willReturn(List.of(result));

        List<ResultDTO.Response> responses = resultService.getResultsByStudent(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStudentName()).isEqualTo("Bob Kumar");
        assertThat(responses.get(0).getCourseCode()).isEqualTo("CS301");
        assertThat(responses.get(0).getGrade()).isEqualTo("A");
        assertThat(responses.get(0).getSemester()).isEqualTo(4);
        then(resultRepository).should().findByStudentId(10L);
    }

    @Test
    @DisplayName("getResultsByStudent — throws ResourceNotFoundException when student not found")
    void getResultsByStudent_throwsWhenStudentNotFound() {
        given(studentRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> resultService.getResultsByStudent(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        then(resultRepository).should(never()).findByStudentId(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET RESULTS BY STUDENT + SEMESTER  (composite index path)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getResultsByStudentAndSemester — uses composite index path, returns filtered results")
    void getResultsByStudentAndSemester_returnsFilteredResults() {
        given(resultRepository.findByStudentIdAndSemester(10L, 4)).willReturn(List.of(result));

        List<ResultDTO.Response> responses = resultService.getResultsByStudentAndSemester(10L, 4);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getSemester()).isEqualTo(4);
        // Verifies the composite-index repository method is called (not the non-indexed fallback)
        then(resultRepository).should().findByStudentIdAndSemester(10L, 4);
        then(studentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("getResultsByStudentAndSemester — returns empty list when no results")
    void getResultsByStudentAndSemester_returnsEmptyList() {
        given(resultRepository.findByStudentIdAndSemester(10L, 8)).willReturn(List.of());

        List<ResultDTO.Response> responses = resultService.getResultsByStudentAndSemester(10L, 8);

        assertThat(responses).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADD RESULT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addResult — persists and returns response on success")
    void addResult_persistsAndReturnsResponse() {
        ResultDTO.Request request = ResultDTO.Request.builder()
                .studentId(10L).courseId(5L).semester(4)
                .marks(new BigDecimal("87.50")).grade("A")
                .examDate(LocalDate.of(2024, 5, 20))
                .build();

        given(resultRepository.existsByStudentIdAndCourseIdAndSemester(10L, 5L, 4)).willReturn(false);
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(courseRepository.findById(5L)).willReturn(Optional.of(course));
        given(resultRepository.save(any(Result.class))).willReturn(result);

        ResultDTO.Response response = resultService.addResult(request);

        assertThat(response.getStudentName()).isEqualTo("Bob Kumar");
        assertThat(response.getCourseName()).isEqualTo("Data Structures");
        assertThat(response.getMarks()).isEqualByComparingTo("87.50");
        then(resultRepository).should().save(any(Result.class));
    }

    @Test
    @DisplayName("addResult — throws DuplicateResourceException on duplicate student+course+semester")
    void addResult_throwsOnDuplicate() {
        ResultDTO.Request request = ResultDTO.Request.builder()
                .studentId(10L).courseId(5L).semester(4)
                .marks(new BigDecimal("87.50")).grade("A")
                .examDate(LocalDate.of(2024, 5, 20))
                .build();

        given(resultRepository.existsByStudentIdAndCourseIdAndSemester(10L, 5L, 4)).willReturn(true);

        assertThatThrownBy(() -> resultService.addResult(request))
                .isInstanceOf(DuplicateResourceException.class);

        then(resultRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("addResult — throws ResourceNotFoundException when student missing")
    void addResult_throwsWhenStudentMissing() {
        ResultDTO.Request request = ResultDTO.Request.builder()
                .studentId(999L).courseId(5L).semester(4)
                .marks(new BigDecimal("87.50")).grade("A")
                .examDate(LocalDate.of(2024, 5, 20))
                .build();

        given(resultRepository.existsByStudentIdAndCourseIdAndSemester(999L, 5L, 4)).willReturn(false);
        given(studentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.addResult(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student");

        then(resultRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("addResult — throws ResourceNotFoundException when course missing")
    void addResult_throwsWhenCourseMissing() {
        ResultDTO.Request request = ResultDTO.Request.builder()
                .studentId(10L).courseId(999L).semester(4)
                .marks(new BigDecimal("87.50")).grade("A")
                .examDate(LocalDate.of(2024, 5, 20))
                .build();

        given(resultRepository.existsByStudentIdAndCourseIdAndSemester(10L, 999L, 4)).willReturn(false);
        given(studentRepository.findById(10L)).willReturn(Optional.of(student));
        given(courseRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.addResult(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course");

        then(resultRepository).should(never()).save(any());
    }
}

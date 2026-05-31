package com.studentvault.service;

import com.studentvault.dto.ResultDTO;
import com.studentvault.exception.DuplicateResourceException;
import com.studentvault.exception.ResourceNotFoundException;
import com.studentvault.model.Course;
import com.studentvault.model.Result;
import com.studentvault.model.Student;
import com.studentvault.repository.CourseRepository;
import com.studentvault.repository.ResultRepository;
import com.studentvault.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultService {

    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<ResultDTO.Response> getResultsByStudent(Long studentId) {
        // Uses idx_results_student_id
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return resultRepository.findByStudentId(studentId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResultDTO.Response> getResultsByStudentAndSemester(Long studentId, Integer semester) {
        // Uses composite index idx_results_student_semester — the key optimization
        return resultRepository.findByStudentIdAndSemester(studentId, semester)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResultDTO.TopperResponse> getToppers(Long departmentId, Integer semester, int limit) {
        // Uses idx_results_semester_marks + JOIN with idx_students_dept_semester
        List<Result> toppers = resultRepository.findToppersByDepartmentAndSemester(
                departmentId, semester, PageRequest.of(0, limit));
        AtomicInteger rank = new AtomicInteger(1);
        return toppers.stream().map(r -> ResultDTO.TopperResponse.builder()
                .studentName(r.getStudent().getName())
                .rollNumber(r.getStudent().getRollNumber())
                .courseName(r.getCourse().getName())
                .marks(r.getMarks())
                .grade(r.getGrade())
                .rank(rank.getAndIncrement())
                .build()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResultDTO.CourseAverage> getCourseAverages(Long departmentId) {
        return resultRepository.findAverageMarksByCourse(departmentId)
                .stream().map(row -> ResultDTO.CourseAverage.builder()
                        .courseName((String) row[0])
                        .averageMarks(((Number) row[1]).doubleValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResultDTO.GradeDistribution> getGradeDistribution(Integer semester) {
        return resultRepository.findGradeDistributionBySemester(semester)
                .stream().map(row -> ResultDTO.GradeDistribution.builder()
                        .grade((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ResultDTO.Response addResult(ResultDTO.Request request) {
        if (resultRepository.existsByStudentIdAndCourseIdAndSemester(
                request.getStudentId(), request.getCourseId(), request.getSemester())) {
            throw new DuplicateResourceException(
                    "Result already exists for this student, course and semester");
        }
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with id: " + request.getStudentId()));
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not found with id: " + request.getCourseId()));
        Result result = Result.builder()
                .student(student)
                .course(course)
                .semester(request.getSemester())
                .marks(request.getMarks())
                .grade(request.getGrade())
                .examDate(request.getExamDate())
                .build();
        return toResponse(resultRepository.save(result));
    }

    private ResultDTO.Response toResponse(Result r) {
        return ResultDTO.Response.builder()
                .id(r.getId())
                .studentId(r.getStudent().getId())
                .studentName(r.getStudent().getName())
                .rollNumber(r.getStudent().getRollNumber())
                .courseName(r.getCourse().getName())
                .courseCode(r.getCourse().getCode())
                .semester(r.getSemester())
                .marks(r.getMarks())
                .grade(r.getGrade())
                .examDate(r.getExamDate())
                .build();
    }
}

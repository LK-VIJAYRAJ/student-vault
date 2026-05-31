package com.studentvault.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ResultDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {
        @NotNull(message = "Student ID is required")
        private Long studentId;

        @NotNull(message = "Course ID is required")
        private Long courseId;

        @NotNull(message = "Semester is required")
        @Min(1) @Max(8)
        private Integer semester;

        @NotNull(message = "Marks are required")
        @DecimalMin("0.00") @DecimalMax("100.00")
        private BigDecimal marks;

        @NotBlank(message = "Grade is required")
        private String grade;

        @NotNull(message = "Exam date is required")
        private LocalDate examDate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private Long studentId;
        private String studentName;
        private String rollNumber;
        private String courseName;
        private String courseCode;
        private Integer semester;
        private BigDecimal marks;
        private String grade;
        private LocalDate examDate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TopperResponse {
        private String studentName;
        private String rollNumber;
        private String courseName;
        private BigDecimal marks;
        private String grade;
        private Integer rank;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CourseAverage {
        private String courseName;
        private Double averageMarks;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GradeDistribution {
        private String grade;
        private Long count;
    }
}

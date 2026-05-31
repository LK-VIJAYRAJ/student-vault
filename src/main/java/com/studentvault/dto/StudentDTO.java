package com.studentvault.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

public class StudentDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {
        @NotBlank(message = "Name is required")
        @Size(max = 150)
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Roll number is required")
        private String rollNumber;

        @NotNull(message = "Department ID is required")
        private Long departmentId;

        @NotNull(message = "Semester is required")
        @Min(1) @Max(8)
        private Integer semester;

        private LocalDate dateOfBirth;

        @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
        private String phone;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String name;
        private String email;
        private String rollNumber;
        private String departmentName;
        private String departmentCode;
        private Integer semester;
        private LocalDate dateOfBirth;
        private String phone;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Summary {
        private Long id;
        private String name;
        private String rollNumber;
        private String departmentCode;
        private Integer semester;
    }
}

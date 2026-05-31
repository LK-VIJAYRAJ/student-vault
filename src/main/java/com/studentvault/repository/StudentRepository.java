package com.studentvault.repository;

import com.studentvault.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Uses idx_students_roll_number — O(log N) instead of O(N)
    Optional<Student> findByRollNumber(String rollNumber);

    // Uses idx_students_dept_semester composite index
    Page<Student> findByDepartmentIdAndSemester(Long departmentId, Integer semester, Pageable pageable);

    // Uses idx_students_department_id
    Page<Student> findByDepartmentId(Long departmentId, Pageable pageable);

    // Uses idx_students_semester
    Page<Student> findBySemester(Integer semester, Pageable pageable);

    // Full-text search across name, email, rollNumber
    @Query("""
            SELECT s FROM Student s
            WHERE (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:departmentId IS NULL OR s.department.id = :departmentId)
              AND (:semester IS NULL OR s.semester = :semester)
            """)
    Page<Student> searchStudents(
            @Param("name") String name,
            @Param("departmentId") Long departmentId,
            @Param("semester") Integer semester,
            Pageable pageable
    );

    // Count students per department — used in analytics
    @Query("SELECT s.department.code, COUNT(s) FROM Student s GROUP BY s.department.code")
    java.util.List<Object[]> countByDepartment();

    boolean existsByEmail(String email);

    boolean existsByRollNumber(String rollNumber);
}

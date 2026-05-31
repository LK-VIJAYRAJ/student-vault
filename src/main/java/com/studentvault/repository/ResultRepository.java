package com.studentvault.repository;

import com.studentvault.model.Result;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    // Uses idx_results_student_id index
    List<Result> findByStudentId(Long studentId);

    // Uses idx_results_student_semester composite index — the KEY optimization story
    List<Result> findByStudentIdAndSemester(Long studentId, Integer semester);

    // Uses idx_results_course_id
    Page<Result> findByCourseId(Long courseId, Pageable pageable);

    // Topper query: top students in a department for a given semester
    // Uses idx_results_semester_marks + JOIN on students with idx_students_dept_semester
    @Query("""
            SELECT r FROM Result r
            JOIN FETCH r.student s
            JOIN FETCH r.course c
            WHERE s.department.id = :departmentId
              AND r.semester = :semester
            ORDER BY r.marks DESC
            """)
    List<Result> findToppersByDepartmentAndSemester(
            @Param("departmentId") Long departmentId,
            @Param("semester") Integer semester,
            Pageable pageable
    );

    // Average marks per course — aggregation query
    @Query("""
            SELECT c.name, AVG(r.marks)
            FROM Result r
            JOIN r.course c
            WHERE c.department.id = :departmentId
            GROUP BY c.name
            ORDER BY AVG(r.marks) DESC
            """)
    List<Object[]> findAverageMarksByCourse(@Param("departmentId") Long departmentId);

    // Grade distribution — useful for analytics dashboard
    @Query("""
            SELECT r.grade, COUNT(r)
            FROM Result r
            WHERE r.semester = :semester
            GROUP BY r.grade
            ORDER BY r.grade
            """)
    List<Object[]> findGradeDistributionBySemester(@Param("semester") Integer semester);

    boolean existsByStudentIdAndCourseIdAndSemester(Long studentId, Long courseId, Integer semester);
}

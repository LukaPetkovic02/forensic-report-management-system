package com.example.backend.repository;

import com.example.backend.model.ForensicReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForensicReportRepository extends JpaRepository<ForensicReport, Long> {
}

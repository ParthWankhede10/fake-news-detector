package com.fakenews.detector.repository;

import com.fakenews.detector.entity.Analysis;
import com.fakenews.detector.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    Page<Analysis> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
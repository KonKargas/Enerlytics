package com.kkargas.enerlytics.repository;

import com.kkargas.enerlytics.model.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    boolean existsBySourceFileName(String sourceFileName);
}

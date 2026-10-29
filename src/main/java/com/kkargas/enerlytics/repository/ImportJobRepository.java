package com.kkargas.enerlytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kkargas.enerlytics.importing.ImportStatus;
import com.kkargas.enerlytics.model.ImportJob;

import java.util.List;

public interface ImportJobRepository
        extends JpaRepository<ImportJob, Long> {

    List<ImportJob> findByStatus(ImportStatus status);

    List<ImportJob> findAllByOrderByStartedAtDesc();
    
	@Query("""
	       SELECT j
	       FROM ImportJob j
	       LEFT JOIN FETCH j.dataset
	       """)
	List<ImportJob> findAllWithDataset();

}

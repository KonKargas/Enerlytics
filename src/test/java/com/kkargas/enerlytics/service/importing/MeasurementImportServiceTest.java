
package com.kkargas.enerlytics.service.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.kkargas.enerlytics.importing.ImportStatus;
import com.kkargas.enerlytics.importing.MeasurementImportService;
import com.kkargas.enerlytics.model.EnergyMeasurement;
import com.kkargas.enerlytics.model.ImportJob;
import com.kkargas.enerlytics.repository.DatasetRepository;
import com.kkargas.enerlytics.repository.ImportJobRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
class MeasurementImportServiceTest {

    private static final String TEST_DB =
            "target/enerlytics-test-" + UUID.randomUUID() + ".db";

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:sqlite:" + TEST_DB
        );
    }

    @Autowired
    private MeasurementImportService importService;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldImportMeasurementsIntoSQLite() throws IOException {

        String filename = "test_measurements.txt";

        ImportJob job = importService.importClasspathFile(
                filename,
                "Enerlytics Test Dataset"
        );

        assertEquals(ImportStatus.COMPLETED, job.getStatus());

        assertEquals(5L, job.getTotalRows());
        assertEquals(4L, job.getImportedRows());
        assertEquals(1L, job.getSkippedRows());

        Long datasetId = job.getDataset().getId();

        assertTrue(datasetRepository.existsById(datasetId));
        assertTrue(importJobRepository.existsById(job.getId()));

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM energy_measurements "
                + "WHERE dataset_id = ?",
                Long.class,
                datasetId
        );

        assertEquals(4L, count);

        Double firstKitchen = jdbcTemplate.queryForObject(
                "SELECT kitchen_wh FROM energy_measurements "
                + "WHERE dataset_id = ? ORDER BY measured_at LIMIT 1",
                Double.class,
                datasetId
        );

        assertEquals(0.0, firstKitchen, 0.0001);

        assertThrows(
                IllegalStateException.class,
                () -> importService.importClasspathFile(
                        filename,
                        "Duplicate Dataset"
                )
        );

        assertEquals(1L, datasetRepository.count());

        System.out.println("=== ENERLYTICS IMPORT TEST ===");
        System.out.println("Status: " + job.getStatus());
        System.out.println("Total rows: " + job.getTotalRows());
        System.out.println("Imported: " + job.getImportedRows());
        System.out.println("Skipped: " + job.getSkippedRows());
        System.out.println("SQLite records: " + count);
        System.out.println("Database: " + TEST_DB);
    }
    

	@Autowired
	private EntityManager entityManager;
	
	@Test
	void shouldReadMeasurementTimestampUsingHibernate() throws IOException {

	    ImportJob job = importService.importClasspathFile(
	            "test_measurements.txt",
	            "Timestamp Validation Dataset"
	    );

	    EnergyMeasurement measurement = entityManager.createQuery(
	            """
	            SELECT m
	            FROM EnergyMeasurement m
	            WHERE m.dataset.id = :datasetId
	            ORDER BY m.measuredAt ASC
	            """,
	            EnergyMeasurement.class
	    )
	    .setParameter("datasetId", job.getDataset().getId())
	    .setMaxResults(1)
	    .getSingleResult();

	    assertEquals(
	            LocalDateTime.of(2006, 12, 16, 17, 24),
	            measurement.getMeasuredAt()
	    );
	}

}

package com.kkargas.enerlytics.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.persistence.EntityManager;

import com.kkargas.enerlytics.model.EnergyMeasurement;

@SpringBootTest
class EnergyMeasurementReadTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReadFirstMeasurementCorrectly() {

        EnergyMeasurement measurement =
                entityManager.createQuery(
                        """
                        SELECT m
                        FROM EnergyMeasurement m
                        ORDER BY m.measuredAt ASC
                        """,
                        EnergyMeasurement.class
                )
                .setMaxResults(1)
                .getSingleResult();

        assertNotNull(measurement);

        assertEquals(
                LocalDateTime.of(2006, 12, 16, 17, 24),
                measurement.getMeasuredAt()
        );

        assertEquals(0.0, measurement.getKitchenWh());
        assertEquals(1.0, measurement.getLaundryWh());
        assertEquals(17.0, measurement.getHeatingWh());

        System.out.println("=== ENERLYTICS JPA READ TEST ===");
        System.out.println("Timestamp: " + measurement.getMeasuredAt());
        System.out.println("Kitchen: " + measurement.getKitchenWh());
        System.out.println("Laundry: " + measurement.getLaundryWh());
        System.out.println("Heating: " + measurement.getHeatingWh());
    }
}

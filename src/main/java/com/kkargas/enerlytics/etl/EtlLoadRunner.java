
package com.kkargas.enerlytics.etl;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.kkargas.enerlytics.model.ImportJob;

@Component
@ConditionalOnProperty(
        name = "enerlytics.etl.load.enabled",
        havingValue = "true"
)
public class EtlLoadRunner implements CommandLineRunner {

    private final MeasurementBatchLoader batchLoader;

    public EtlLoadRunner(
            MeasurementBatchLoader batchLoader
    ) {
        this.batchLoader = batchLoader;
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println();
        System.out.println("======================================");
        System.out.println("        ENERLYTICS - ETL LOAD         ");
        System.out.println("======================================");

        Path cleanedFile = Path.of(
                "src/main/resources/data",
                "household_power_cleaned.csv"
        );

        Instant start = Instant.now();

        ImportJob job = batchLoader.load(
                cleanedFile,
                "Household Energy Consumption 2006-2010"
        );

        Duration duration = Duration.between(
                start, Instant.now()
        );

        System.out.println();
        System.out.println("========== LOAD RESULTS ==========");
        System.out.println("Status       : " + job.getStatus());

        System.out.printf(
                "Total rows   : %,d%n",
                job.getTotalRows()
        );

        System.out.printf(
                "Imported     : %,d%n",
                job.getImportedRows()
        );

        System.out.printf(
                "Skipped      : %,d%n",
                job.getSkippedRows()
        );

        System.out.println(
                "Dataset ID   : " + job.getDataset().getId()
        );

        System.out.println(
                "Duration     : " + duration.toSeconds() + " seconds"
        );

        System.out.println("==================================");
    }
}

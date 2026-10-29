
package com.kkargas.enerlytics.etl;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "enerlytics.etl.enabled",
        havingValue = "true"
)
public class EtlRunner implements CommandLineRunner {

    private final EtlService etlService;

    public EtlRunner(EtlService etlService) {
        this.etlService = etlService;
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println();
        System.out.println("====================================");
        System.out.println("       ENERLYTICS ETL PIPELINE      ");
        System.out.println("====================================");

        Path dataDirectory = Path.of(
                "src/main/resources/data"
        );

        System.out.println("Starting ETL process...");
        System.out.println("Data directory: "
                + dataDirectory.toAbsolutePath());

        Instant start = Instant.now();

        EtlResult result = etlService.execute(dataDirectory);

        Duration duration = Duration.between(
                start, Instant.now()
        );

        System.out.println();
        System.out.println("========== ETL RESULTS ==========");
        System.out.printf(
                "Total records    : %,d%n",
                result.totalRows()
        );
        System.out.printf(
                "Cleaned records  : %,d%n",
                result.cleanedRows()
        );
        System.out.printf(
                "Rejected records : %,d%n",
                result.rejectedRows()
        );

        System.out.println("---------------------------------");
        System.out.println("Cleaned file  : "
                + result.cleanedFile());
        System.out.println("Rejected file : "
                + result.rejectedFile());

        System.out.println("---------------------------------");
        System.out.println("Consistent    : "
                + result.isConsistent());
        System.out.println("Duration      : "
                + duration.toMillis() + " ms");
        System.out.println("=================================");
    }
}

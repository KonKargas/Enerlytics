# Enerlytics ⚡

**Enerlytics** is a locally hosted energy-consumption analytics web application built with Java, Spring Boot, Thymeleaf and SQLite. It converts the original university energy-processing exercise into a multi-page dashboard for importing, inspecting, aggregating and reporting household electricity readings.

## Features

- **Dashboard:** dataset/measurement overview, data range and energy-consumption chart.
- **Analytics:** SUM and AVG for kitchen, laundry and heating/AC submetering; group by month, season, weekday or time of day.
- **Date filters and period comparisons:** filter measurements between dates and compare two intervals using charts.
- **Datasets:** inspect registered datasets, import semicolon-separated TXT/CSV files and review import history.
- **ETL monitoring:** processing workflow, job statuses, imported/skipped records and failure messages.
- **Reports:** filtered tabular results, CSV export and browser print/save as PDF.
- **Settings:** browser-local analytics preferences.
- **Responsive interface:** dark navy/gold UI with reusable Thymeleaf fragments and Chart.js visualizations.

## Technology

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.1, Spring MVC |
| Frontend | Thymeleaf, HTML, CSS, JavaScript, Chart.js |
| Persistence | SQLite, Spring Data JPA / Hibernate, JDBC batch operations |
| Build | Maven, Maven Wrapper |
| Tests | JUnit / Spring Boot Test |

## Data model and units

The initial supported source is the **UCI Individual Household Electric Power Consumption** dataset, with the following semicolon-separated columns:

```text
Date;Time;Global_active_power;Global_reactive_power;Voltage;Global_intensity;Sub_metering_1;Sub_metering_2;Sub_metering_3
```

Sub-metering 1 = kitchen, 2 = laundry, 3 = water heater / air conditioning. Sub-metering observations are stored as **Wh per measurement interval**. The reports' SUM is the sum of submeter Wh (converted to kWh when displayed as such); AVG is Wh per valid observation. Monthly, seasonal and weekday grouping combines readings from different years unless a date range is selected.

**Supported upload format:** `.txt` or `.csv`, UTF-8, semicolon-separated, using the exact header above, dates in `d/M/yyyy` format and time in `HH:mm:ss`. Arbitrary CSV schemas are not automatically compatible.

## Run locally

Prerequisites: **JDK 21+**, with a network connection for Maven dependencies on first build.

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

Alternatively use an installed Maven: `mvn spring-boot:run`. Open **http://localhost:8080/dashboard**. The application creates `enerlytics.db` in its *working directory* if it does not exist. Its initial tables will be empty; upload a compatible dataset from **Datasets**.

### Preparing an example file

```csv
Date;Time;Global_active_power;Global_reactive_power;Voltage;Global_intensity;Sub_metering_1;Sub_metering_2;Sub_metering_3
1/1/2011;12:00:00;1.5;0.1;230;6.5;1;2;3
1/1/2011;12:01:00;1.6;0.1;231;6.7;2;3;4
```

Save as `example.csv`, open **Datasets**, supply a dataset name and upload the file. The application performs validation and records an import job. Some malformed rows can be skipped; an import with no valid rows is rejected. Back up your database before importing large files.

### Source dataset

The original UCI dataset spans **December 2006 – November 2010**. Download it from the UCI Machine Learning Repository:

https://archive.ics.uci.edu/dataset/235/individual+household+electric+power+consumption

The original `.txt` and generated cleaned/rejected CSV files are **deliberately excluded from this repository** because they are large. The included ETL command-line runners (disabled by default in `application.properties`) expect data files at `src/main/resources/data/`. You do not need those runners to upload data through the web interface.

## Navigation

| Page | URL |
| --- | --- |
| Dashboard | `/dashboard` |
| Analytics | `/analytics` |
| Datasets | `/datasets` |
| Reports | `/reports` |
| ETL Pipeline | `/etl` |
| Settings | `/settings` |

## Tests

Run unit tests or selected integration tests with Maven or your IDE. **Important:** the following tests were originally written against a pre-populated local SQLite database and are not self-contained:

- `AggregationServiceTest` expects dataset ID 1 with **2,049,280** records.
- `EnergyMeasurementReadTest` expects the first original reading from **2006-12-16 17:24**.

These will not pass against the freshly created empty database. The isolated aggregation calculation/date-filter tests and ETL/parser tests are better starting points on a clean checkout. A complete independent test profile/fixture is future work.

## Project organization

```text
src/main/java/com/kkargas/enerlytics/
├── analytics/      # Aggregation and grouping
├── controller/     # UI and API endpoints
├── etl/            # Extract, clean, batch load and CLI runners
├── importing/      # Parsers and import helpers
├── model/          # Entities and measurement records
├── repository/     # Spring Data repositories
└── service/        # Web dataset import
src/main/resources/
├── static/         # CSS, JavaScript and images
├── templates/      # Thymeleaf pages and shared fragments
└── application.properties
```

## Notes and limitations

- Designed for **local/trusted use**. There is no authentication or authorization; do **not** expose an unrestricted instance to the public Internet.
- Settings preferences are saved in the browser's local storage, not in SQLite.
- PDF output uses the browser print/save-as-PDF function; the application does not currently generate PDF files server-side.
- SQLite and any imported data remain local. Do not commit database files, generated ETL exports or proprietary datasets.

## Author

Developed by **Konstantinos Kargas** — [kkargas.com](https://kkargas.com).

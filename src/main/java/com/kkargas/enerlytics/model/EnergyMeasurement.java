package com.kkargas.enerlytics.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "energy_measurements", indexes = {
    @Index(name = "idx_measurement_dataset_time", columnList = "dataset_id, measured_at")
})
@Getter @Setter @NoArgsConstructor
public class EnergyMeasurement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    // Original source sub-metering columns: watt-hours for the sampling interval.
    @Column(name = "kitchen_wh", nullable = false)
    private Double kitchenWh;

    @Column(name = "laundry_wh", nullable = false)
    private Double laundryWh;

    @Column(name = "heating_wh", nullable = false)
    private Double heatingWh;

    // Optional readings in source dataset; kW and volts respectively.
    @Column(name = "global_active_power_kw")
    private Double globalActivePowerKw;

    @Column(name = "global_reactive_power_kvar")
    private Double globalReactivePowerKvar;

    @Column(name = "global_intensity_a")
    private Double globalIntensityA;

    @Column(name = "voltage_v")
    private Double voltageV;
}

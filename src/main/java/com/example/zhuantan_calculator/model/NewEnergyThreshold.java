package com.example.zhuantan_calculator.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "new_energy_threshold")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewEnergyThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_group")
    private String vehicleGroup;

    @Column(name = "year")
    private Integer year;

    @Column(name = "threshold")
    private Double threshold;
}
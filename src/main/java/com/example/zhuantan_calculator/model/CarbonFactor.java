package com.example.zhuantan_calculator.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "carbon_factor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarbonFactor {

    @Id
    @Column(name = "fuel_type")
    private String fuelType;

    @Column(name = "carbon_factor")
    private Double carbonFactor;
}
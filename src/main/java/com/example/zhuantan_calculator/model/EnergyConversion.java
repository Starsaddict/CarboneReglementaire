package com.example.zhuantan_calculator.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "energy_conversion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnergyConversion {

    @Id
    @Column(name = "fuel_type")
    private String fuelType;

    @Column(name = "gasoline_coeff")
    private Double gasolineCoeff;

    @Column(name = "diesel_coeff")
    private Double dieselCoeff;

    @Column(name = "note")
    private String note;
}
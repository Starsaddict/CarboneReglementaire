package com.example.zhuantan_calculator.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "commercial_target")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommercialTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 为避免无主键问题添加的伪主键

    @Column(name = "carbon_model")
    private String carbonModel;

    @Column(name = "fuel_type")
    private String fuelType;

    @Column(name = "gvw_min")
    private Double gvwMin;

    @Column(name = "gvw_max")
    private Double gvwMax;

    @Column(name = "method")
    private Integer method;

    @Column(name = "year")
    private Integer year;

    @Column(name = "target_value")
    private Double targetValue;

    @Column(name = "gvw_area")
    private String gvwArea;
}
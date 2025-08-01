package com.example.zhuantan_calculator.functionalInterface;

import com.example.zhuantan_calculator.model.Vehicles;

public interface CarbonFactorProvider {
    Double getCarbonFactor(Vehicles vehicle);
}

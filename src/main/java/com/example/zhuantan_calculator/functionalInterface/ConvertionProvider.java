package com.example.zhuantan_calculator.functionalInterface;

import com.example.zhuantan_calculator.model.Vehicles;

public interface ConvertionProvider {
    Double getConvertCoeff(String energyType, String carbonEnergyType, int method);

    Double getConvertCoeff(Vehicles vehicle, int method);
}

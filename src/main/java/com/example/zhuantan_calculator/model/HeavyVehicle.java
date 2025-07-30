package com.example.zhuantan_calculator.model;

import com.example.zhuantan_calculator.functionalInterface.TargetProvider;
public class HeavyVehicle extends Vehicles{

    public HeavyVehicle() { }

    @Override
    public Double doComputeTarget(TargetProvider targetProvider, int method) {
        return targetProvider.getTarget(this,method);
    }

}

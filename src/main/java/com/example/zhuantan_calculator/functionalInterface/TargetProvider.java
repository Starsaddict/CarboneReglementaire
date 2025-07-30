package com.example.zhuantan_calculator.functionalInterface;
import com.example.zhuantan_calculator.model.HeavyVehicle;
public interface TargetProvider {
    double getTarget(HeavyVehicle vehicle, int method);
}

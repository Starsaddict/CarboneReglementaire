package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.model.Vehicles;

import java.util.List;

public class VehicleService {

    public double computePenetrationRate(List<Vehicles> vehicleList, Vehicles target) {
        String enterprise = target.getEnterprise();
        int year = target.getYear();
        String model = target.getModel();

        long total = vehicleList.stream()
                .filter(v -> v.getEnterprise().equals(enterprise)
                        && v.getYear() == year
                        && v.getModel().equals(model))
                .count();

        long newEnergyCount = vehicleList.stream()
                .filter(v -> v.getEnterprise().equals(enterprise)
                        && v.getYear() == year
                        && v.getModel().equals(model)
                        && v.isNewEnergy())
                .count();

        return total == 0 ? 0.0 : (double) newEnergyCount / total;
    }

}
package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.model.Vehicles;

import java.util.List;

public class VehicleService {

    public double computePenetrationRate(List<Vehicles> vehicleList, Vehicles target) {
        String enterprise = target.getEnterprise();
        int year = target.getYear();
        String group = target.getCarbonGroup();
        // 按销量累加计算总销量
        double totalSales = vehicleList.stream()
                .filter(v -> v.getEnterprise().equals(enterprise)
                        && v.getYear() == year
                        && v.getCarbonGroup().equals(group))
                .mapToDouble(Vehicles::getSales)
                .sum();

        // 按销量累加计算新能源销量
        double newEnergySales = vehicleList.stream()
                .filter(v -> v.getEnterprise().equals(enterprise)
                        && v.getYear() == year
                        && v.getCarbonGroup().equals(group)
                        && v.isNewEnergy())
                .mapToDouble(Vehicles::getSales)
                .sum();

        return totalSales == 0.0 ? 0.0 : newEnergySales / totalSales;
    }

}
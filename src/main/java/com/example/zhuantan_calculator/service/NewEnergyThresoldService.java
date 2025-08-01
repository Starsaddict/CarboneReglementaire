package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.repository.NewEnergyThresholdRepo;
import jakarta.persistence.EntityManager;

public class NewEnergyThresoldService {
    private final EntityManager entityManager;

    public NewEnergyThresoldService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Double computeNewEnergyThreshold(int year, String vehicleGroup){
        NewEnergyThresholdRepo newEnergyThresholdRepo = new NewEnergyThresholdRepo(entityManager);
        if(year == 2030 |year == 2028 | year == 2029){
             return newEnergyThresholdRepo.findThreshold(year, vehicleGroup);
        }
        return null;
    }
}

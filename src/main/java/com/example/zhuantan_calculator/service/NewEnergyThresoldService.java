package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.model.NewEnergyThreshold;
import com.example.zhuantan_calculator.repository.NewEnergyThresholdRepo;
import jakarta.persistence.EntityManager;

import java.util.List;

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

    public List<NewEnergyThreshold> getAllEnergyThreshold(){
        NewEnergyThresholdRepo newEnergyThresholdRepo = new NewEnergyThresholdRepo(entityManager);
        return newEnergyThresholdRepo.findAll();
    }
}

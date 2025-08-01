package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.model.Vehicles;
import com.example.zhuantan_calculator.repository.CarbonFactorRepo;
import jakarta.persistence.EntityManager;

public class CarbonFactorService {
    private final EntityManager entityManager;

    public CarbonFactorService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public double getCarbonFactor(String carbonEnergyType){
        CarbonFactorRepo carbonFactorRepo = new CarbonFactorRepo(entityManager);
        return carbonFactorRepo.findCarbonFactor(carbonEnergyType);
    }

}

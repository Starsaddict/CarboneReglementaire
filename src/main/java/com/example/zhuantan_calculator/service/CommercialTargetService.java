package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.repository.CommercialTargetRepo;
import jakarta.persistence.EntityManager;

public class CommercialTargetService {

    private final EntityManager entityManager;

    public CommercialTargetService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public double getTarget(int year, String carbonModel, String fuelType, Integer gvm, String gvwArea, int method){ //TODO: 这里有bug
        CommercialTargetRepo commercialTargetRepo = new CommercialTargetRepo(entityManager);

        if(gvm != null){
            return commercialTargetRepo.findTargetValue(year, carbonModel, fuelType, gvm, method);
        }else{
            try{
            return commercialTargetRepo.findTargetValue(year,carbonModel,fuelType,gvwArea,method);}
            catch(Exception e){
                return 100;
            }
        }
    }
}

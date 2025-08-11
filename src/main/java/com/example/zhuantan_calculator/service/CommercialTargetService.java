package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.repository.CommercialTargetRepo;
import jakarta.persistence.EntityManager;

import java.util.List;

public class CommercialTargetService {

    private final EntityManager entityManager;

    public CommercialTargetService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public double getTarget(int year, String carbonModel, String fuelType, Integer gvm, String gvwArea, int method){
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

    public List<String> listGvwAreasByCarbonGroup(String carbonGroup) {
        if("N1".equals(carbonGroup) || "M2".equals(carbonGroup)){
            return null;
        }
        CommercialTargetRepo commercialTargetRepo = new CommercialTargetRepo(entityManager);
        if("轻型载货".equals(carbonGroup)||"中重型载货".equals(carbonGroup)){
            return commercialTargetRepo.findAllGVWAreasByCarbonGroup("货车");
        }
        return commercialTargetRepo.findAllGVWAreasByCarbonGroup(carbonGroup);
    }
}

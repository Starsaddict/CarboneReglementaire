package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.model.CommercialTarget;
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

        if(!(gvm == null)){
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

    public String ifMatchGVMArea(String carbonGroup, Integer grossWeight, String gvwArea){
        List<String> areas = listGvwAreasByCarbonGroup(carbonGroup);


//        System.out.println("grossWeight:"+ grossWeight+ "gvwArea:"+gvwArea);

        if((grossWeight == null || grossWeight.equals(0)) && gvwArea == null)
            return "质量段和总质量不能同时为空";


        String carbonModel = ("轻型载货".equals(carbonGroup) || "中重型载货".equals(carbonGroup)) ? "货车" : carbonGroup;

        if((gvwArea!=null) && !areas.contains(gvwArea)){
            return "该质量段不存在";
        }

        if(gvwArea!=null && grossWeight!=null){
            CommercialTargetRepo commercialTargetRepo = new CommercialTargetRepo(entityManager);

            if(!gvwArea.equals(commercialTargetRepo.findGVWAreaByGVW(grossWeight,carbonModel))){
                return "质量段与质量不对应，计算时以总质量数据为准";
            }
        }

        return "ok";

    }

    public List<CommercialTarget> getAllCommercialTarget(){
        CommercialTargetRepo carbonTargetRepo = new CommercialTargetRepo(entityManager);
        return carbonTargetRepo.findAll();
    }
}

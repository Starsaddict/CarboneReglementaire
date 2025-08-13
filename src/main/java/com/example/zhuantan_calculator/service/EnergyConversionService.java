package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.model.EnergyConversion;
import com.example.zhuantan_calculator.repository.EnergyConversionRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;

public class EnergyConversionService {
    private final EntityManager entityManager;

    public EnergyConversionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    // 这里对于energyType为天然气的车：要提前把energyType改为天然气-LNG或者天然气-CNG
    public Double computeConversionCoeff(String energyType, String carbonEnergyType, int method){
        EnergyConversionRepo energyConversionRepo = new EnergyConversionRepo(entityManager);
        if("汽油".equals(energyType) || "柴油".equals(energyType)){
            return 1.0;
        }
        if(("BEV".equals(energyType) || "电".equals(energyType)) && method == 0) {
            return 0.0;
        }else if(("BEV".equals(energyType) || "电".equals(energyType)) && method == 1) {
            if("汽油".equals(carbonEnergyType)){
                return energyConversionRepo.findGasCoeff("BEV_method1");
            }else if("柴油".equals(carbonEnergyType)){
                return energyConversionRepo.findDieselCoeff("BEV_method1");
            }
        }else if(("BEV".equals(energyType) || "电".equals(energyType)) & method == 3) {
            if("汽油".equals(carbonEnergyType)){
                return energyConversionRepo.findGasCoeff("BEV_method3");
            }else if("柴油".equals(carbonEnergyType)){
                return energyConversionRepo.findDieselCoeff("BEV_method3");
            }
        }
        else if("汽油".equals(carbonEnergyType)){
            return energyConversionRepo.findGasCoeff(energyType);
        } else if ("柴油".equals(carbonEnergyType)) {
            return energyConversionRepo.findDieselCoeff(energyType);
        }
        return null ;
    }

    public List<EnergyConversion> getAllEnergyConversion(){
        EnergyConversionRepo energyConversionRepo = new EnergyConversionRepo(entityManager);
        return energyConversionRepo.findAall();
    }

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("zhuantanPU");
        EntityManager em = emf.createEntityManager();
        EnergyConversionService energyConversionService = new EnergyConversionService(em);
        Double result = energyConversionService.computeConversionCoeff("柴油", "柴油", 1);
        System.out.println(result);
    }

}

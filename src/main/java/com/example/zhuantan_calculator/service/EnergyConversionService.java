package com.example.zhuantan_calculator.service;

import com.example.zhuantan_calculator.repository.EnergyConversionRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class EnergyConversionService {
    private final EntityManager entityManager;

    public EnergyConversionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    // 这里对于energyType为天然气的车：要提前把energyType改为天然气-LNG或者天然气-CNG
    public Double computeConversionCoeff(String energyType, String carbonEnergyType, int method){
        EnergyConversionRepo energyConversionRepo = new EnergyConversionRepo(entityManager);
        if(energyType.equals("BEV") && method == 0) {
            return 0.0;
        }else if(energyType.equals("BEV") && method == 1) {
            if(carbonEnergyType.equals("汽油")){
                return energyConversionRepo.findGasCoeff("BEV_method1");
            }else if(carbonEnergyType.equals("柴油")){
                return energyConversionRepo.findDieselCoeff("BEV_method1");
            }
        }else if(energyType.equals("BEV") & method == 3) {
            if(carbonEnergyType.equals("汽油")){
                return energyConversionRepo.findGasCoeff("BEV_method3");
            }else if(carbonEnergyType.equals("柴油")){
                return energyConversionRepo.findDieselCoeff("BEV_method3");
            }
        }
        else if(carbonEnergyType.equals("汽油")){
            return energyConversionRepo.findGasCoeff(energyType);
        } else if (carbonEnergyType.equals("柴油")) {
            return energyConversionRepo.findDieselCoeff(energyType);
        }
        return null ;
    }

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("zhuantanPU");
        EntityManager em = emf.createEntityManager();
        EnergyConversionService energyConversionService = new EnergyConversionService(em);
        Double result = energyConversionService.computeConversionCoeff("汽油", "柴油", 0);
        System.out.println(result);
    }

}

package com.example.zhuantan_calculator.repository;

import com.example.zhuantan_calculator.model.CommercialTarget;
import com.example.zhuantan_calculator.model.EnergyConversion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class EnergyConversionRepo {
    private final EntityManager entityManager;

    public EnergyConversionRepo(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<EnergyConversion> findAall(){
        try {
            return entityManager.createQuery("From EnergyConversion", EnergyConversion.class).getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Double findDieselCoeff(String fuelType){
        try {
            return entityManager
                    .createQuery("select e.dieselCoeff from EnergyConversion e " +
                            "where fuelType = :fuelType ", Double.class)
                    .setParameter("fuelType", fuelType)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Double findGasCoeff(String fuelType){
        try {
            return entityManager.createQuery(
                    "select e.gasolineCoeff from EnergyConversion e " +
                            "where fuelType = :fuelType ", Double.class)
                    .setParameter("fuelType",fuelType )
                    .getSingleResult();
        } catch (NoResultException e){
            return null;
        }
    }

}

package com.example.zhuantan_calculator.repository;

import com.example.zhuantan_calculator.model.NewEnergyThreshold;
import jakarta.persistence.EntityManager;

import jakarta.persistence.NoResultException;


import java.util.List;

public class NewEnergyThresholdRepo {
    private final EntityManager entityManager;

    public NewEnergyThresholdRepo(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<NewEnergyThreshold> findAll() {
        try {
            return entityManager.createQuery("from NewEnergyThreshold", NewEnergyThreshold.class).getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Double findThreshold(int year, String vehicleGroup){
        try {
            return entityManager.createQuery(
                    "select n.threshold from NewEnergyThreshold n " +
                            "where year = :year " +
                            "and vehicleGroup = :vehicleGroup"
                            ,Double.class)
                    .setParameter("year", year)
                    .setParameter("vehicleGroup", vehicleGroup).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }


}

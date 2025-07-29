package com.example.zhuantan_calculator.repository;

import com.example.zhuantan_calculator.model.CarbonFactor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;

import java.util.List;

public class CarbonFactorRepo {
    private final EntityManager entityManager;

    public CarbonFactorRepo(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // 示例方法
    public void save(CarbonFactor factor) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            entityManager.persist(factor);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        }
    }

    public List<CarbonFactor> findAll() {
        return entityManager.createQuery("from CarbonFactor", CarbonFactor.class).getResultList();
    }

    public Double findCarbonFactor(String fuelType) {
        try {
            return entityManager
                    .createQuery("SELECT c.carbonFactor FROM CarbonFactor c WHERE c.fuelType = :fuelType", Double.class)
                    .setParameter("fuelType", fuelType)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
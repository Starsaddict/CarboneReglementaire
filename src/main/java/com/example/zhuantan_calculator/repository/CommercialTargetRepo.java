package com.example.zhuantan_calculator.repository;

import com.example.zhuantan_calculator.model.CommercialTarget;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class CommercialTargetRepo {
    private final EntityManager entityManager;

    public CommercialTargetRepo(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<CommercialTarget> findAll() {
        return entityManager.createQuery("from CommercialTarget", CommercialTarget.class).getResultList();
    }

    public Double findTargetValue (int year, String carbonModel, String fuelType, int gvm, int method){
        try {
            return entityManager
                    .createQuery("select c.targetValue from CommercialTarget c " +
                            "where c.year = :year " +
                            "and c.carbonModel = :carbonModel " +
                            "and c.fuelType = :fuelType " +
                            "and c.gvwMax >= :gvm " +
                            "and c.gvwMin < :gvm " +
                            "and c.method = :method", Double.class)
                    .setParameter("year",year)
                    .setParameter("carbonModel",carbonModel)
                    .setParameter("fuelType",fuelType)
                    .setParameter("gvm",gvm)
                    .setParameter("method",method)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Double findTargetValue (int year, String carbonModel, String fuelType, String gvwArea, int method){
        try {
            return entityManager
                    .createQuery("select c.targetValue from CommercialTarget c " +
                            "where c.year = :year " +
                            "and c.carbonModel = :carbonModel " +
                            "and c.fuelType = :fuelType " +
                            "and c.gvwArea = :gvwArea " +
                            "and c.method = :method", Double.class)
                    .setParameter("year",year)
                    .setParameter("carbonModel",carbonModel)
                    .setParameter("fuelType",fuelType)
                    .setParameter("gvwArea",gvwArea)
                    .setParameter("method",method)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<String> findAllGVWAreasByCarbonGroup(String carbonModel) {
        try {
            return entityManager.createQuery(
                    "select distinct gvwArea from CommercialTarget " +
                            "where carbonModel = :carbonModel " +
                            "order by gvwMin"
                    , String.class)
                    .setParameter("carbonModel",carbonModel)
                    .getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }
}
